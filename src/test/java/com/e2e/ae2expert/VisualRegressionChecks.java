package com.e2e.ae2expert;

import com.e2e.ae2expert.core.ExpertTransformer;
import com.e2e.ae2expert.part.PartExpertCable;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Проверяем настоящий байткод модов и передачу граничных значений, а не макеты реализации. */
final class VisualRegressionChecks {
    private static int checks;
    private static void check(boolean value, String text) {
        checks++;
        if (!value) throw new AssertionError(text);
    }
    static int run() throws Exception {
        ExpertTransformer transformer = new ExpertTransformer();
        for (String name : Arrays.asList("appeng.client.render.cablebus.CableBusBakedModel",
                "appeng.integration.modules.waila.part.ChannelWailaDataProvider",
                "appeng.integration.modules.theoneprobe.part.ChannelInfoProvider",
                "appeng.parts.networking.PartDenseCable",
                "appeng.container.implementations.ContainerUpgradeable")) {
            byte[] original = bytes(name), patched = transformer.transform(name,name,original);
            check(!Arrays.equals(original,patched), "Применён визуальный патч " + name);
            ClassNode node = new ClassNode(); new ClassReader(patched).accept(node,0);
            if (name.endsWith("CableBusBakedModel")) {
                int unsafe = 0, safe = 0;
                for (MethodNode method:node.methods) if (method.name.equals("<clinit>"))
                    for (AbstractInsnNode instruction:method.instructions.toArray()) if(instruction instanceof TypeInsnNode) {
                        String type=((TypeInsnNode)instruction).desc;
                        if(type.equals("java/util/HashMap"))unsafe++;
                        if(type.equals("java/util/concurrent/ConcurrentHashMap"))safe++;
                    }
                check(unsafe==0 && safe==1,"Кэш рендера AE2 использует ConcurrentHashMap");
            }
        }
        for (int capacity:new int[]{32,64,128,256}) {
            check(PartExpertCable.displaySegments(capacity,capacity)==32,"Полная шкала " + capacity);
            check(PartExpertCable.displaySegments(capacity/2,capacity)==16,"Половина шкалы " + capacity);
            check(PartExpertCable.displaySegments(1,capacity)==1,"Непустой кабель виден " + capacity);
            check(PartExpertCable.displaySegments(0,capacity)==0,"Пустая шкала " + capacity);
        }
        Map<String,byte[]> classes=new HashMap<>();
        try(JarFile archive=new JarFile("libs/ae2stuff-0.7.0.4-mc1.12.2.jar")) {
            Enumeration<JarEntry> entries=archive.entries();
            while(entries.hasMoreElements()) {
                JarEntry entry=entries.nextElement();
                if(!entry.getName().startsWith("net/bdew/ae2stuff/items/visualiser/")||!entry.getName().endsWith(".class"))continue;
                String name=entry.getName().replace('/','.').replace(".class","");
                byte[] data=bytes(name); classes.put(name,transformer.transform(name,name,data));
            }
        }
        ClassLoader loader=new ClassLoader(VisualRegressionChecks.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!classes.containsKey(name))return super.loadClass(name,resolve);
                Class<?> type=findLoadedClass(name);
                if(type==null){byte[] data=classes.get(name);type=defineClass(name,data,0,data.length);}
                if(resolve)resolveClass(type);return type;
            }
        };
        Class<?> nodeType=loader.loadClass("net.bdew.ae2stuff.items.visualiser.VNode");
        Class<?> linkType=loader.loadClass("net.bdew.ae2stuff.items.visualiser.VLink");
        Class<?> dataType=loader.loadClass("net.bdew.ae2stuff.items.visualiser.VisualisationData");
        Object nodeFlags=emptyFlags(loader,"VNodeFlags$");
        Object linkFlags=emptyFlags(loader,"VLinkFlags$");
        Class<?> flags=Class.forName("scala.Enumeration$ValueSet");
        Object first=nodeType.getConstructor(int.class,int.class,int.class,flags).newInstance(0,0,0,nodeFlags);
        Object second=nodeType.getConstructor(int.class,int.class,int.class,flags).newInstance(1,0,0,nodeFlags);
        Constructor<?> linkConstructor=linkType.getConstructor(nodeType,nodeType,int.class,flags);
        Method getter=linkType.getMethod("channels");
        check(getter.getReturnType()==int.class,"Визуализатор хранит число каналов как int");
        // Реальные сериализатор и десериализатор AE2 Stuff, включая два соседних байта флагов.
        for(int count:new int[]{0,1,32,64,127,128,255,256}) {
            Object link=linkConstructor.newInstance(first,second,count,linkFlags);
            check((int)getter.invoke(link)==count,"VLink без переполнения " + count);
            Object source=dataType.getConstructor(scala.collection.Seq.class,scala.collection.Seq.class)
                    .newInstance(seq(first,second),seq(link));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            ObjectOutputStream output=new ObjectOutputStream(bytes);
            dataType.getMethod("writeExternal",ObjectOutput.class).invoke(source,output);output.flush();
            Object restored=dataType.getConstructor().newInstance();
            dataType.getMethod("readExternal",ObjectInput.class).invoke(restored,new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            scala.collection.Seq<?> links=(scala.collection.Seq<?>)dataType.getMethod("links").invoke(restored);
            check((int)getter.invoke(links.apply(0))==count,"Передача визуализатора сохраняет " + count);
            check(link.equals(links.apply(0)),"Узлы и флаги не повреждены " + count);
            check(linkType.getMethod("productElement",int.class).invoke(link,2).equals(count),"Подпись визуализатора положительна " + count);
        }
        return checks;
    }
    private static scala.collection.Seq<?> seq(Object... values) {
        return scala.collection.JavaConverters.asScalaBufferConverter(Arrays.asList(values)).asScala().toSeq();
    }
    private static Object emptyFlags(ClassLoader loader,String name)throws Exception {
        Object module=loader.loadClass("net.bdew.ae2stuff.items.visualiser."+name).getField("MODULE$").get(null);
        Object factory=module.getClass().getMethod("ValueSet").invoke(module);
        return factory.getClass().getMethod("fromBitMask",long[].class).invoke(factory,new long[]{0});
    }
    private static byte[] bytes(String name)throws Exception {
        try(InputStream in=VisualRegressionChecks.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")) {
            if(in==null)throw new IllegalStateException(name);
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();
        }
    }
}

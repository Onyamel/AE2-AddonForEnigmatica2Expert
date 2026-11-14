package com.e2e.ae2expert.core;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Точечные изменения доступа к фильтрам и крепления частей на плотных кабелях AE2 rv6. */
final class FeatureTransformers {
    static byte[] transform(String name, byte[] original) {
        boolean dense=name.equals("appeng.parts.networking.PartDenseCable");
        boolean container=name.equals("appeng.container.implementations.ContainerUpgradeable");
        if(!dense&&!container)return original;
        ClassNode type=new ClassNode();new ClassReader(original).accept(type,0);
        int changed=0;
        for(MethodNode method:type.methods) {
            if(dense&&method.name.equals("isDense")&&method.desc.equals("(Lappeng/api/util/AEPartLocation;)Z")) {
                // У прикреплённых частей появляется INTERNAL: у него нет EnumFacing и соседнего блока.
                LabelNode external=new LabelNode();InsnList guard=new InsnList();
                guard.add(new VarInsnNode(Opcodes.ALOAD,1));
                guard.add(new FieldInsnNode(Opcodes.GETSTATIC,"appeng/api/util/AEPartLocation","INTERNAL","Lappeng/api/util/AEPartLocation;"));
                guard.add(new JumpInsnNode(Opcodes.IF_ACMPNE,external));
                guard.add(new InsnNode(Opcodes.ICONST_0));guard.add(new InsnNode(Opcodes.IRETURN));
                guard.add(external);guard.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(guard);changed++;
            }
            if(dense&&method.name.equals("supportsBuses")&&method.desc.equals("()Lappeng/api/parts/BusSupport;")) {
                // Меняется только совместимость крепления. Флаги DENSE_CAPACITY и геометрия сохраняются.
                for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof FieldInsnNode) {
                    FieldInsnNode field=(FieldInsnNode)instruction;
                    if(field.owner.equals("appeng/api/parts/BusSupport")&&field.name.equals("DENSE_CABLE")) {field.name="CABLE";changed++;}
                }
            }
            if(container&&method.name.equals("isSlotEnabled")&&method.desc.equals("(I)Z")) {
                // Добавляем быстрый путь только нашим шинам; ванильные карты и фильтры не меняем.
                LabelNode vanilla=new LabelNode();InsnList hook=new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD,0));
                hook.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,"upgradeable","Lappeng/api/implementations/IUpgradeableHost;"));
                hook.add(new TypeInsnNode(Opcodes.INSTANCEOF,"com/e2e/ae2expert/api/INineSlotBus"));
                hook.add(new JumpInsnNode(Opcodes.IFEQ,vanilla));
                hook.add(new VarInsnNode(Opcodes.ILOAD,1));hook.add(new InsnNode(Opcodes.ICONST_1));
                LabelNode enabled=new LabelNode();hook.add(new JumpInsnNode(Opcodes.IF_ICMPEQ,enabled));
                hook.add(new VarInsnNode(Opcodes.ILOAD,1));hook.add(new InsnNode(Opcodes.ICONST_2));hook.add(new JumpInsnNode(Opcodes.IF_ICMPNE,vanilla));
                hook.add(enabled);hook.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));hook.add(new InsnNode(Opcodes.ICONST_1));hook.add(new InsnNode(Opcodes.IRETURN));
                hook.add(vanilla);hook.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(hook);changed++;
            }
        }
        if(changed!=(dense?2:1))throw new IllegalStateException("Не найдены все точки патча " + name + ", найдено: " + changed);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);
        System.out.println("[AE2Expert] Патч возможностей: "+name);
        return writer.toByteArray();
    }
}

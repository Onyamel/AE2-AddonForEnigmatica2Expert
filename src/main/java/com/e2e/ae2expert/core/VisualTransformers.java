package com.e2e.ae2expert.core;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Совместимость рендера и числовой телеметрии; сетевой pathfinding не меняется. */
final class VisualTransformers {
    private static final String DISPLAY = "com/e2e/ae2expert/core/DisplayChannels";
    private static final String CLIENT = "com/e2e/ae2expert/client/ClientVisuals";
    private static final String VLINK = "net/bdew/ae2stuff/items/visualiser/VLink";

    static byte[] transform(String name, byte[] original) {
        boolean cable = name.equals("appeng.client.render.cablebus.CableBusBakedModel");
        boolean waila = name.equals("appeng.integration.modules.waila.part.ChannelWailaDataProvider");
        boolean probe = name.equals("appeng.integration.modules.theoneprobe.part.ChannelInfoProvider");
        boolean visualiser = name.startsWith("net.bdew.ae2stuff.items.visualiser.");
        if (!cable && !waila && !probe && !visualiser) return original;
        ClassNode type = new ClassNode();
        new ClassReader(original).accept(type, 0);
        int changes = 0;
        boolean linkType = type.name.equals(VLINK) || type.name.equals(VLINK + "$");
        if (type.name.equals(VLINK)) for (FieldNode field : type.fields) {
            if (field.name.equals("channels") && field.desc.equals("B")) { field.desc = "I"; changes++; }
        }
        for (MethodNode method : type.methods) {
            if (linkType) method.desc = method.desc.replace("B", "I");
            if (waila && method.name.equals("getWailaBody")) {
                InsnList body = new InsnList();
                body.add(new VarInsnNode(Opcodes.ALOAD, 1));
                body.add(new VarInsnNode(Opcodes.ALOAD, 2));
                body.add(new VarInsnNode(Opcodes.ALOAD, 3));
                body.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "mcp/mobius/waila/api/IWailaDataAccessor",
                        "getNBTData", "()Lnet/minecraft/nbt/NBTTagCompound;", true));
                body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DISPLAY, "tooltip",
                        "(Lappeng/api/parts/IPart;Ljava/util/List;Lnet/minecraft/nbt/NBTTagCompound;)Ljava/util/List;", false));
                body.add(new InsnNode(Opcodes.ARETURN));
                replaceBody(method, body); changes++; continue;
            }
            if (waila && method.name.equals("getNBTData")) {
                InsnList body = new InsnList();
                body.add(new VarInsnNode(Opcodes.ALOAD, 2)); body.add(new VarInsnNode(Opcodes.ALOAD, 4));
                body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DISPLAY, "writeTag",
                        "(Lappeng/api/parts/IPart;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;", false));
                body.add(new InsnNode(Opcodes.ARETURN)); replaceBody(method, body); changes++; continue;
            }
            boolean linkMethod = linkType;
            boolean readsLink = false;
            for (AbstractInsnNode insn : method.instructions.toArray()) if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode)insn;
                if (call.owner.equals(VLINK)) { linkMethod = true; if (call.name.equals("<init>")) readsLink = true; }
            }
            boolean readCount = false, writeCount = false;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (probe && insn instanceof IntInsnNode && (((IntInsnNode)insn).operand == 32 || ((IntInsnNode)insn).operand == 8)) {
                    InsnList replacement = new InsnList(); replacement.add(new VarInsnNode(Opcodes.ALOAD,1));
                    replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,DISPLAY,"capacity","(Lappeng/api/parts/IPart;)I",false));
                    method.instructions.insertBefore(insn,replacement);method.instructions.remove(insn);changes++;continue;
                }
                if (cable && method.name.equals("<clinit>")) {
                    if (insn instanceof TypeInsnNode && ((TypeInsnNode)insn).desc.equals("java/util/HashMap")) {
                        ((TypeInsnNode)insn).desc="java/util/concurrent/ConcurrentHashMap";changes++;
                    }
                    if (insn instanceof MethodInsnNode && ((MethodInsnNode)insn).owner.equals("java/util/HashMap")) {
                        ((MethodInsnNode)insn).owner="java/util/concurrent/ConcurrentHashMap";changes++;
                    }
                }
                if (cable && (method.name.equals("getQuads") || method.name.equals("func_188616_a")) && insn.getOpcode()==Opcodes.ARETURN) {
                    InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,1));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,CLIENT,"tintCableQuads",
                            "(Ljava/util/List;Lnet/minecraft/block/state/IBlockState;)Ljava/util/List;",false));
                    method.instructions.insertBefore(insn,hook);changes++;
                }
                if(cable&&method.name.equals("addCableQuads")&&insn.getOpcode()==Opcodes.RETURN) {
                    InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,0));
                    hook.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,"cableBuilder","Lappeng/client/render/cablebus/CableBuilder;"));
                    hook.add(new VarInsnNode(Opcodes.ALOAD,1));hook.add(new VarInsnNode(Opcodes.ALOAD,2));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"appeng/client/render/cablebus/ExpertDenseAttachmentBridge","add",
                            "(Lappeng/client/render/cablebus/CableBuilder;Lappeng/client/render/cablebus/CableBusRenderState;Ljava/util/List;)V",false));
                    method.instructions.insertBefore(insn,hook);changes++;
                }
                if (visualiser && insn instanceof FieldInsnNode) {
                    FieldInsnNode field=(FieldInsnNode)insn;
                    if(field.owner.equals(VLINK)&&field.name.equals("channels")&&field.desc.equals("B")){field.desc="I";changes++;}
                }
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call=(MethodInsnNode)insn;
                if(probe && call.owner.equals("net/minecraft/nbt/NBTTagCompound") && call.desc.equals("(Ljava/lang/String;)B")) {
                    call.name=call.name.startsWith("func_")?"func_74762_e":"getInteger";call.desc="(Ljava/lang/String;)I";changes++;
                }
                if (!visualiser) continue;
                if (call.owner.equals(VLINK)||call.owner.equals(VLINK+"$")) {
                    String expanded=call.desc.replace("B","I");
                    if(!expanded.equals(call.desc)){call.desc=expanded;changes++;}
                    if(call.name.equals("channels"))writeCount=true;
                }
                if(linkMethod && call.owner.equals("scala/runtime/BoxesRunTime")) {
                    if(call.name.equals("boxToByte")){call.name="boxToInteger";call.desc="(I)Ljava/lang/Integer;";changes++;}
                    if(call.name.equals("unboxToByte")){call.name="unboxToInt";call.desc="(Ljava/lang/Object;)I";changes++;}
                }
                if(call.owner.equals("appeng/api/networking/IGridConnection") && call.name.equals("getUsedChannels")) {
                    AbstractInsnNode next=call.getNext();while(next!=null&&next.getOpcode()<0)next=next.getNext();
                    if(next!=null&&next.getOpcode()==Opcodes.I2B){method.instructions.remove(next);changes++;}
                }
                if(readsLink && !readCount && call.owner.equals("java/io/ObjectInput") && call.name.equals("readByte")) {
                    call.name="readUnsignedShort";call.desc="()I";readCount=true;changes++;
                }
                if(writeCount && call.owner.equals("java/io/ObjectOutput") && call.name.equals("writeByte")) {
                    call.name="writeShort";writeCount=false;changes++;
                }
                if (call.owner.equals("org/lwjgl/opengl/GL11") && (call.name.equals("glColor3f")||call.name.equals("glColor3d"))) {
                    call.owner=CLIENT;call.name=call.name.equals("glColor3f")?"quietColor3f":"quietColor3d";changes++;
                }
            }
        }
        if(changes==0)return original;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);
        System.out.println("[AE2Expert] Визуальный патч: "+name+", изменений: "+changes);
        return writer.toByteArray();
    }
    private static void replaceBody(MethodNode method,InsnList body) {
        method.instructions=body;method.tryCatchBlocks.clear();method.localVariables=null;
        method.maxStack=5;
    }
}

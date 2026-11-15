package com.e2e.ae2expert.core;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

final class ExtendedMatrixTransformer implements Opcodes {
    private static final String MATRIX = "net/minecraft/inventory/InventoryCrafting";
    private static final String PATTERN = "appeng/api/networking/crafting/ICraftingPatternDetails";

    private ExtendedMatrixTransformer() { }

    static byte[] transform(byte[] bytes, boolean cpu) {
        ClassNode type = new ClassNode();
        new ClassReader(bytes).accept(type, 0);
        int changes = 0;
        for (MethodNode method : type.methods) {
            if (cpu ? !method.name.equals("executeCrafting")
                    : !method.name.equals("<init>") && !method.name.equals("request")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.owner.equals(MATRIX) || !call.name.equals("<init>")
                        || !call.desc.equals("(Lnet/minecraft/inventory/Container;II)V")) continue;
                AbstractInsnNode start = call.getPrevious();
                while (start != null && !(start.getOpcode() == NEW && ((TypeInsnNode) start).desc.equals(MATRIX)))
                    start = start.getPrevious();
                if (start == null || start.getNext().getOpcode() != DUP)
                    throw new IllegalStateException("Unexpected AE2 crafting matrix allocation");
                method.instructions.remove(start.getNext());
                method.instructions.remove(start);
                InsnList details = new InsnList();
                if (cpu) {
                    int patternLocal = -1;
                    for (AbstractInsnNode candidate : method.instructions.toArray()) {
                        if (candidate instanceof MethodInsnNode && ((MethodInsnNode) candidate).owner.equals(PATTERN)
                                && ((MethodInsnNode) candidate).name.equals("getInputs")
                                && candidate.getPrevious() instanceof VarInsnNode
                                && candidate.getPrevious().getOpcode() == ALOAD) {
                            patternLocal = ((VarInsnNode) candidate.getPrevious()).var;
                            break;
                        }
                    }
                    if (patternLocal < 0) throw new IllegalStateException("Missing AE2 crafting pattern local");
                    details.add(new VarInsnNode(ALOAD, patternLocal));
                } else if (method.name.equals("<init>")) {
                    details.add(new VarInsnNode(ALOAD, 3));
                } else {
                    details.add(new VarInsnNode(ALOAD, 0));
                    details.add(new FieldInsnNode(GETFIELD, type.name, "details", "L" + PATTERN + ";"));
                }
                method.instructions.insertBefore(call, details);
                method.instructions.set(call, new MethodInsnNode(INVOKESTATIC,
                        "com/e2e/ae2expert/extended/PatternMatrix", "create",
                        "(Lnet/minecraft/inventory/Container;IIL" + PATTERN + ";)L" + MATRIX + ";", false));
                changes++;
            }
        }
        if (changes != (cpu ? 1 : 2)) throw new IllegalStateException("Unexpected AE2 matrix count: " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }
}

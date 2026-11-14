package com.e2e.ae2expert.core;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Extends only capacity accounting in AE2 rv6's original crafting cluster. */
final class CraftingCpuTransformer implements Opcodes {
    private static final String CLUSTER = "appeng/me/cluster/implementations/CraftingCPUCluster";
    private static final String TILE = "appeng/tile/crafting/TileCraftingTile";
    private static final String HOOK = "com/e2e/ae2expert/crafting/CpuAccounting";

    static byte[] transform(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        int storage = 0;
        int accelerators = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("addTile") || !method.desc.equals("(L" + TILE + ";)V")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    AbstractInsnNode next = call.getNext();
                    if (call.owner.equals(TILE) && call.name.equals("getStorageBytes") && call.desc.equals("()I")
                            && next != null && next.getOpcode() == I2L) {
                        method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, HOOK, "bytes", "(L" + TILE + ";)J", false));
                        method.instructions.remove(next);
                        storage++;
                    }
                    if (call.owner.equals(TILE) && call.name.equals("isStorage") && call.desc.equals("()Z")) {
                        // The tile receiver is already on the stack. Leave it there for isStorage().
                        InsnList add = new InsnList();
                        add.add(new VarInsnNode(ALOAD, 0));
                        add.add(new InsnNode(DUP));
                        add.add(new FieldInsnNode(GETFIELD, CLUSTER, "accelerator", "I"));
                        add.add(new VarInsnNode(ALOAD, 1));
                        add.add(new MethodInsnNode(INVOKESTATIC, HOOK, "processors", "(IL" + TILE + ";)I", false));
                        add.add(new FieldInsnNode(PUTFIELD, CLUSTER, "accelerator", "I"));
                        method.instructions.insertBefore(call, add);
                        accelerators++;
                    }
                }
            }
        }
        if (storage != 1 || accelerators != 1)
            throw new IllegalStateException("Unsupported AE2 crafting CPU layout: " + storage + "/" + accelerators);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[AE2Expert] Crafting CPU capacity accounting enabled");
        return writer.toByteArray();
    }

    static byte[] transformCache(byte[] original) {
        String cache = "appeng/me/cache/CraftingGridCache";
        String grid = "Lappeng/api/networking/IGrid;";
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        int added = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("updateCPUClusters") || !method.desc.equals("()V")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != RETURN) continue;
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new FieldInsnNode(GETFIELD, cache, "grid", grid));
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new FieldInsnNode(GETFIELD, cache, "craftingCPUClusters", "Ljava/util/Set;"));
                hook.add(new MethodInsnNode(INVOKESTATIC, "com/e2e/ae2expert/crafting/CpuDiscovery",
                        "addExpertCpus", "(L" + cache + ";" + grid + "Ljava/util/Set;)V", false));
                method.instructions.insertBefore(instruction, hook);
                added++;
            }
        }
        if (added != 1) throw new IllegalStateException("Unsupported AE2 CPU discovery layout: " + added);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[AE2Expert] Expert crafting CPU discovery enabled");
        return writer.toByteArray();
    }
}

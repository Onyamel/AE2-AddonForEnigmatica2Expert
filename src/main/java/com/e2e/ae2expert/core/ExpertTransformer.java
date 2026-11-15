package com.e2e.ae2expert.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Applies the two surgical patches described in {@link ExpertLoadingPlugin}.
 *
 * <p>Every patch is applied inside a try/catch and only when the expected instruction sequence is
 * actually found. If AE2 changes or the class is already patched, the original bytes are returned,
 * so a mismatch can never corrupt a class.</p>
 */
public class ExpertTransformer implements IClassTransformer {

    private static final String GRID_NODE = "appeng.me.GridNode";
    private static final String PART_CABLE = "appeng.parts.networking.PartCable";
    private static final String GRID_CONNECTION = "appeng.me.GridConnection";

    private static final String ITEM_PART = "appeng/items/parts/ItemPart";
    private static final String HOOK = "com/e2e/ae2expert/core/ChannelHook";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if ("appeng.me.cluster.implementations.CraftingCPUCluster".equals(transformedName))
            return ExtendedMatrixTransformer.transform(CraftingCpuTransformer.transform(basicClass), true);
        if ("appeng.crafting.CraftingTreeProcess".equals(transformedName))
            return ExtendedMatrixTransformer.transform(basicClass, false);
        if ("appeng.me.cache.CraftingGridCache".equals(transformedName))
            return CraftingCpuTransformer.transformCache(basicClass);
        try {
            if (GRID_NODE.equals(transformedName)) {
                return this.patchGridNode(basicClass);
            }
            if (PART_CABLE.equals(transformedName)) {
                return this.patchPartCable(basicClass);
            }
            if (GRID_CONNECTION.equals(transformedName)) return patchConnection(basicClass);
        } catch (Throwable t) {
            System.err.println("[AE2Expert] Patch failed for " + transformedName + " - keeping original: " + t);
        }
        try { return VisualTransformers.transform(transformedName, FeatureTransformers.transform(transformedName,basicClass)); }
        catch (Throwable failure) {
            System.err.println("[AE2Expert] Визуальный патч пропущен для " + transformedName + ": " + failure);
            return basicClass;
        }
    }

    /** Два 16-битных счётчика вместо двух байтов и отдельная ёмкость ребра. */
    private byte[] patchConnection(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int masks = 0, shifts = 0, capacity = 0;
        for (MethodNode method : node.methods) {
            if ("canSupportMoreChannels".equals(method.name) && "()Z".equals(method.desc)) {
                method.instructions = new InsnList();
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK,
                        "connectionCanCarry", "(Ljava/lang/Object;)Z", false));
                method.instructions.add(new InsnNode(Opcodes.IRETURN));
                method.tryCatchBlocks.clear();
                method.localVariables = null;
                capacity++;
            }
            boolean counter = "getUsedChannels".equals(method.name) || "getLastUsedChannels".equals(method.name)
                    || "setControllerRoute".equals(method.name) || "finalizeChannels".equals(method.name);
            if (!counter) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;) {
                AbstractInsnNode next = insn.getNext();
                Integer value = null;
                if (insn instanceof IntInsnNode) value = ((IntInsnNode) insn).operand;
                else if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Integer)
                    value = (Integer) ((LdcInsnNode) insn).cst;
                if (value != null && (value == 255 || value == -256)) {
                    method.instructions.set(insn, new LdcInsnNode(value == 255 ? 65535 : -65536));
                    masks++;
                } else if (value != null && value == 8 && next != null
                        && (next.getOpcode() == Opcodes.ISHR || next.getOpcode() == Opcodes.ISHL)) {
                    method.instructions.set(insn, new IntInsnNode(Opcodes.BIPUSH, 16));
                    shifts++;
                }
                insn = next;
            }
        }
        if (masks != 4 || shifts != 2 || capacity != 1)
            throw new IllegalStateException("Unexpected GridConnection layout: " + masks + "/" + shifts + "/" + capacity);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[AE2Expert] Patched GridConnection: 16-bit counters and cable capacity");
        return writer.toByteArray();
    }

    /**
     * Replaces the body of {@code private int getMaxChannels()} with a call to the hook.
     * Original body: {@code return CHANNEL_COUNT[compressedData & 3];}
     */
    private byte[] patchGridNode(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if ("getMaxChannels".equals(method.name) && "()I".equals(method.desc)) {
                target = method;
                break;
            }
        }
        if (target == null) {
            System.err.println("[AE2Expert] GridNode.getMaxChannels() not found, channel patch skipped");
            return basicClass;
        }

        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "getMaxChannels",
                "(Ljava/lang/Object;)I", false));
        replacement.add(new InsnNode(Opcodes.IRETURN));

        target.instructions = replacement;
        target.tryCatchBlocks.clear();
        target.localVariables = null;
        target.maxStack = 2;
        target.maxLocals = 1;

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[AE2Expert] Patched GridNode.getMaxChannels() for custom cable capacity");
        return writer.toByteArray();
    }

    /**
     * Relaxes the {@code (ItemPart) item} cast and redirects {@code ItemPart.variantOf(int)} to the
     * hook, so the addon's cable item (which is not an AE2 ItemPart) can be placed.
     */
    private byte[] patchPartCable(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode ctor = null;
        for (MethodNode method : node.methods) {
            if ("<init>".equals(method.name) && "(Lnet/minecraft/item/ItemStack;)V".equals(method.desc)) {
                ctor = method;
                break;
            }
        }
        if (ctor == null) {
            System.err.println("[AE2Expert] PartCable constructor not found, colour patch skipped");
            return basicClass;
        }

        boolean relaxedCast = false;
        boolean redirectedCall = false;
        for (AbstractInsnNode insn = ctor.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof TypeInsnNode) {
                TypeInsnNode typeInsn = (TypeInsnNode) insn;
                if (typeInsn.getOpcode() == Opcodes.CHECKCAST && ITEM_PART.equals(typeInsn.desc)) {
                    typeInsn.desc = "java/lang/Object";
                    relaxedCast = true;
                }
            } else if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && ITEM_PART.equals(call.owner)
                        && "variantOf".equals(call.name) && "(I)I".equals(call.desc)) {
                    ctor.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "variantOf",
                            "(Ljava/lang/Object;I)I", false));
                    redirectedCall = true;
                }
            }
        }

        if (!relaxedCast || !redirectedCall) {
            System.err.println("[AE2Expert] PartCable colour sequence not found (cast=" + relaxedCast
                    + ", call=" + redirectedCall + "), colour patch skipped");
            return basicClass;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[AE2Expert] Patched PartCable constructor for addon cable items");
        return writer.toByteArray();
    }
}

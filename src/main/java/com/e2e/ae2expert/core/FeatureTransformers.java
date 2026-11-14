package com.e2e.ae2expert.core;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Точечные изменения доступа к фильтрам и крепления частей на плотных кабелях AE2 rv6. */
final class FeatureTransformers {
    static byte[] transform(String name, byte[] original) {
        boolean dense=name.equals("appeng.parts.networking.PartDenseCable");
        boolean container=name.equals("appeng.container.implementations.ContainerUpgradeable");
        boolean workbench=name.equals("appeng.container.implementations.ContainerCellWorkbench");
        boolean assembler=name.equals("appeng.tile.crafting.TileMolecularAssembler");
        if(!dense&&!container&&!workbench&&!assembler)return original;
        ClassNode type=new ClassNode();new ClassReader(original).accept(type,0);
        int changed=0;
        for(MethodNode method:type.methods) {
            if (assembler && method.name.equals("updateSleepiness") && method.desc.equals("()V")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("appeng/api/networking/ticking/ITickManager") && call.name.equals("sleepDevice")
                            && call.desc.equals("(Lappeng/api/networking/IGridNode;)Z")) {
                        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "com/e2e/ae2expert/assembler/TileExpertAssembler", "sleepUnlessQueued",
                                "(Lappeng/api/networking/ticking/ITickManager;Lappeng/api/networking/IGridNode;)Z", false));
                        changed++;
                    }
                }
            }
            if(workbench&&method.name.equals("partition")&&method.desc.equals("()V")) {
                LabelNode vanilla=new LabelNode();InsnList hook=new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD,0));
                hook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,type.name,"getUpgradeable",
                        "()Lappeng/api/implementations/IUpgradeableHost;",false));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/e2e/ae2expert/core/CellWorkbenchHook",
                        "partition","(Lappeng/api/implementations/IUpgradeableHost;)Z",false));
                hook.add(new JumpInsnNode(Opcodes.IFEQ,vanilla));
                hook.add(new VarInsnNode(Opcodes.ALOAD,0));
                // Имя метода Minecraft различается в dev и SRG; ищем существующий вызов.
                String sync="detectAndSendChanges";
                for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if(call.desc.equals("()V")&&(call.name.equals("func_75142_b")||call.name.equals("detectAndSendChanges")))sync=call.name;
                }
                hook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,type.name,sync,"()V",false));
                hook.add(new InsnNode(Opcodes.RETURN));
                hook.add(vanilla);hook.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(hook);changed++;
            }
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

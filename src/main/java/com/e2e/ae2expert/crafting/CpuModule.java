package com.e2e.ae2expert.crafting;

import appeng.block.crafting.BlockCraftingUnit;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

final class CpuModule {
    private CpuModule() { }

    private static Block block(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlock
                ? ((ItemBlock) stack.getItem()).getBlock() : null;
    }

    static int bytes(ItemStack stack) {
        Block block = block(stack);
        if (block instanceof BlockExpertCpu) return ((BlockExpertCpu) block).memory;
        if (!(block instanceof BlockCraftingUnit)) return 0;
        switch (((BlockCraftingUnit) block).type) {
            case STORAGE_1K: return 1024;
            case STORAGE_4K: return 4096;
            case STORAGE_16K: return 16384;
            case STORAGE_64K: return 65536;
            default: return 0;
        }
    }

    static int processors(ItemStack stack) {
        Block block = block(stack);
        if (block instanceof BlockExpertCpu) return ((BlockExpertCpu) block).processors;
        return block instanceof BlockCraftingUnit
                && ((BlockCraftingUnit) block).type == BlockCraftingUnit.CraftingUnitType.ACCELERATOR ? 1 : 0;
    }

    static boolean accepts(ItemStack stack) {
        return !stack.hasTagCompound() && stack.getMetadata() == 0
                && (bytes(stack) > 0 || processors(stack) > 0);
    }
}

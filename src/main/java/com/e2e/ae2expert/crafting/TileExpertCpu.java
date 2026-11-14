package com.e2e.ae2expert.crafting;

import appeng.tile.crafting.TileCraftingTile;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class TileExpertCpu extends TileCraftingTile implements ExpertCpuCapacity {
    protected final BlockExpertCpu cpuBlock() {
        if (world == null || isInvalid()) return null;
        Block block = getBlockType();
        return block instanceof BlockExpertCpu ? (BlockExpertCpu) block : null;
    }

    @Override
    protected ItemStack getItemFromTile(Object tile) {
        BlockExpertCpu block = cpuBlock();
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    @Override
    public int getStorageBytes() {
        // AE2 uses this int method to decide whether a multiblock has storage.
        return (int) Math.min(Integer.MAX_VALUE, craftingBytes());
    }

    @Override public boolean isStorage() { return craftingBytes() > 0; }
    @Override public boolean isAccelerator() { return false; }

    @Override
    public long craftingBytes() {
        BlockExpertCpu block = cpuBlock();
        return block == null ? 0 : block.memory;
    }

    @Override
    public int coprocessors() {
        BlockExpertCpu block = cpuBlock();
        return block == null ? 0 : block.processors;
    }
}

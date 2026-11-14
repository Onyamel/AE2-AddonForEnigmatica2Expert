package com.e2e.ae2expert.crafting;

import appeng.tile.crafting.TileCraftingTile;

public final class CpuAccounting {
    private CpuAccounting() { }

    public static long bytes(TileCraftingTile tile) {
        return tile instanceof ExpertCpuCapacity ? ((ExpertCpuCapacity) tile).craftingBytes() : tile.getStorageBytes();
    }

    public static int processors(int current, TileCraftingTile tile) {
        if (!(tile instanceof ExpertCpuCapacity)) return current;
        // A 17-cube cluster can contain enough fully loaded racks to overflow int.
        // Reserve headroom for AE2's base operation and subsequent stock units.
        return (int) Math.min(Integer.MAX_VALUE - 8192L,
                (long) current + ((ExpertCpuCapacity) tile).coprocessors());
    }
}

package com.e2e.ae2expert.extended;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;

/** Keeps AE2's usual matrix for existing patterns and preserves positions for extended patterns. */
public final class PatternMatrix {
    private PatternMatrix() { }

    public interface Sized {
        int matrixWidth();
    }

    public static InventoryCrafting create(Container owner, int width, int height, ICraftingPatternDetails pattern) {
        if (pattern instanceof Sized) {
            int size = ((Sized) pattern).matrixWidth();
            if (size != 3 && size != 5 && size != 7 && size != 9)
                throw new IllegalArgumentException("Invalid extended crafting matrix: " + size);
            return new InventoryCrafting(owner, size, size);
        }
        return new InventoryCrafting(owner, width, height);
    }
}

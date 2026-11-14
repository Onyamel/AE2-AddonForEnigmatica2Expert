package com.e2e.ae2expert.machine;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Helpers to resolve AE2 items by registry name, so the addon never crashes when AE2 is configured
 * differently than expected.
 */
public final class ModMachineItems {

    private ModMachineItems() {
    }

    public static ItemStack fluixCrystal() {
        return ae2Material(7, 2);
    }

    public static ItemStack ae2Material(int meta, int count) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation("appliedenergistics2", "material"));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count, meta);
    }
}

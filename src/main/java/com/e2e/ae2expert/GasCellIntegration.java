package com.e2e.ae2expert;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.e2e.ae2expert.item.ItemExpertGasCell;
import com.e2e.ae2expert.storage.CellTier;

/**
 * Gas cells depend on ExtraCells2. This class is only loaded when ExtraCells is present, so the
 * addon keeps working (without gas cells) when it is not.
 */
final class GasCellIntegration {

    static final Map<CellTier, ItemExpertGasCell> gasCells = new EnumMap<>(CellTier.class);

    private GasCellIntegration() {
    }

    static void create() {
        for (CellTier tier : CellTier.values()) {
            ItemExpertGasCell cell = new ItemExpertGasCell(tier);
            ModItems.register(cell);
            gasCells.put(tier, cell);
        }
    }

    static ItemStack stack(CellTier tier) {
        Item item = gasCells.get(tier);
        if (item == null) throw new IllegalStateException("Gas cell not registered: " + tier);
        return new ItemStack(item);
    }
}

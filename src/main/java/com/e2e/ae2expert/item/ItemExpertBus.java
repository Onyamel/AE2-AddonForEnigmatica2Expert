package com.e2e.ae2expert.item;

import net.minecraft.item.ItemStack;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.items.AEBaseItem;

import com.e2e.ae2expert.bus.BusTier;
import com.e2e.ae2expert.part.PartExpertExportBus;
import com.e2e.ae2expert.part.PartExpertImportBus;

/**
 * Item form of the addon's import/export buses.
 */
public class ItemExpertBus extends ExpertPartItem {

    private final BusTier tier;
    private final boolean export;

    public ItemExpertBus(BusTier tier, boolean export) {
        this.tier = tier;
        this.export = export;
        String family = export ? "export_bus" : "import_bus";
        this.setRegistryName("ae2expert", family + "_" + tier.id);
        this.setTranslationKey("ae2expert." + family + "_" + tier.id);
        this.setMaxStackSize(64);
    }

    public BusTier getTier() {
        return this.tier;
    }

    public boolean isExport() {
        return this.export;
    }

    @Override
    public IPart createPartFromItemStack(ItemStack is) {
        return this.export ? new PartExpertExportBus(is) : new PartExpertImportBus(is);
    }
}

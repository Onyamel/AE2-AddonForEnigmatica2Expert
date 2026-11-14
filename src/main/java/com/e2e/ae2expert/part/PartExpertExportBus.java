package com.e2e.ae2expert.part;

import net.minecraft.item.ItemStack;

import appeng.api.parts.IPartModel;
import appeng.parts.automation.PartExportBus;

import com.e2e.ae2expert.bus.BusModels;
import com.e2e.ae2expert.item.ItemExpertBus;

/**
 * Export bus of the addon: a stock AE2 export bus that moves several items per operation.
 */
public class PartExpertExportBus extends PartExportBus implements com.e2e.ae2expert.api.INineSlotBus {

    private final int multiplier;

    public PartExpertExportBus(ItemStack is) {
        super(is);
        this.multiplier = PartExpertImportBus.resolveMultiplier(is);
    }

    @Override
    protected int calculateItemsToSend() {
        return super.calculateItemsToSend() * this.multiplier;
    }

    @Override protected int availableSlots() { return 9; }

    @Override
    public IPartModel getStaticModels() {
        IPartModel[] models = BusModels.exportModels(this.multiplier);
        if (models == null) {
            return super.getStaticModels();
        }
        if (this.isActive() && this.isPowered()) {
            return models[2];
        }
        if (this.isPowered()) {
            return models[1];
        }
        return models[0];
    }
}

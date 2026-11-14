package com.e2e.ae2expert.part;

import net.minecraft.item.ItemStack;

import appeng.api.parts.IPartModel;
import appeng.parts.automation.PartImportBus;

import com.e2e.ae2expert.bus.BusModels;
import com.e2e.ae2expert.item.ItemExpertBus;

/**
 * Import bus of the addon: a stock AE2 import bus that moves several items per operation.
 */
public class PartExpertImportBus extends PartImportBus implements com.e2e.ae2expert.api.INineSlotBus {

    private final int multiplier;

    public PartExpertImportBus(ItemStack is) {
        super(is);
        this.multiplier = resolveMultiplier(is);
    }

    static int resolveMultiplier(ItemStack is) {
        if (is.getItem() instanceof ItemExpertBus) {
            return ((ItemExpertBus) is.getItem()).getTier().multiplier;
        }
        return 1;
    }

    @Override
    protected int calculateItemsToSend() {
        return super.calculateItemsToSend() * this.multiplier;
    }

    @Override protected int availableSlots() { return 9; }

    @Override
    public IPartModel getStaticModels() {
        IPartModel[] models = BusModels.importModels(this.multiplier);
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

package com.e2e.ae2expert.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import extracells.api.gas.IAEGasStack;
import extracells.api.gas.IGasStorageChannel;
import com.e2e.ae2expert.storage.CellTier;

/** Ячейка с безопасными счётчиками long и фильтрами AE2. */
public final class ItemExpertGasCell extends ExpertStorageCell<IAEGasStack> {
    public ItemExpertGasCell(CellTier tier) { super("gas_cell_" + tier.getId(), tier); }
    @Override public IStorageChannel<IAEGasStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class);
    }
}

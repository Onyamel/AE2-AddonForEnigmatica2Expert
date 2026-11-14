package com.e2e.ae2expert.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.channels.IFluidStorageChannel;
import com.e2e.ae2expert.storage.CellTier;

/** Ячейка с безопасными счётчиками long и фильтрами AE2. */
public final class ItemExpertFluidCell extends ExpertStorageCell<IAEFluidStack> {
    public ItemExpertFluidCell(CellTier tier) { super("fluid_cell_" + tier.getId(), tier); }
    @Override public IStorageChannel<IAEFluidStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
    }
}

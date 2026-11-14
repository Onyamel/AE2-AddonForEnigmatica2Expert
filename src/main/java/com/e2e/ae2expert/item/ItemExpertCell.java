package com.e2e.ae2expert.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.channels.IItemStorageChannel;
import com.e2e.ae2expert.storage.CellTier;

/** Ячейка с безопасными счётчиками long и фильтрами AE2. */
public final class ItemExpertCell extends ExpertStorageCell<IAEItemStack> {
    public ItemExpertCell(CellTier tier) { super("cell_" + tier.getId(), tier); }
    @Override public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }
}

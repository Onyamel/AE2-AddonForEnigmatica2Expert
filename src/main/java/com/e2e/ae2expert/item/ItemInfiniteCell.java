package com.e2e.ae2expert.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import com.e2e.ae2expert.storage.InfiniteCellInventory;

/** Практически неограниченная ячейка на 1024 типа. */
public final class ItemInfiniteCell extends ExpertStorageCell<IAEItemStack> {
    public ItemInfiniteCell() {
        super("cell_infinite", null, InfiniteCellInventory.TOTAL_BYTES, InfiniteCellInventory.MAX_TYPES, 1024, 8);
    }
    @Override public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }
}

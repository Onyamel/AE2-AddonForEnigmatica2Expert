package com.e2e.ae2expert.storage;

import appeng.api.AEApi;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;

/** Совместимый формат бесконечной ячейки с общим проверенным инвентарём. */
public final class InfiniteCellInventory extends LongCellInventory<IAEItemStack> {
    public static final int MAX_TYPES = 1024;
    public static final long TOTAL_BYTES = 1L << 50;
    public InfiniteCellInventory(ItemStack stack, ISaveProvider provider) {
        super(stack, provider, AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class),
                TOTAL_BYTES, MAX_TYPES, 1024, 8);
    }
}

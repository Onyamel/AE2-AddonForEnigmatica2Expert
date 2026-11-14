package com.e2e.ae2expert.core;

import java.util.Iterator;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.util.helpers.ItemHandlerUtil;
import com.e2e.ae2expert.item.ExpertStorageCell;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Штатный верстак AE2 должен определять канал и у ячеек с long-ёмкостью. */
public final class CellWorkbenchHook {
    private CellWorkbenchHook() { }

    public static boolean partition(IUpgradeableHost host) {
        ItemStack stack = host.getInventoryByName("cell").getStackInSlot(0);
        if (!(stack.getItem() instanceof ExpertStorageCell)) return false;
        partitionCell((ExpertStorageCell<?>) stack.getItem(), stack, host.getInventoryByName("config"));
        return true;
    }

    private static <T extends IAEStack<T>> void partitionCell(ExpertStorageCell<T> item,
            ItemStack stack, IItemHandler config) {
        IStorageChannel<T> channel = item.getChannel();
        ICellInventoryHandler<T> inventory = item.getCellInventory(stack, null, channel);
        // Отказ обработчика не должен очищать уже настроенную конфигурацию.
        if (inventory == null) return;
        Iterator<T> entries = inventory.getAvailableItems(channel.createList()).iterator();
        for (int slot = 0; slot < config.getSlots(); slot++) {
            ItemStack sample = entries.hasNext() ? entries.next().asItemStackRepresentation() : ItemStack.EMPTY;
            ItemHandlerUtil.setStackInSlot(config, slot, sample);
        }
    }
}

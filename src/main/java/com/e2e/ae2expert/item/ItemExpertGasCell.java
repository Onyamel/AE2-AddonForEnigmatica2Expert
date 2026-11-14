package com.e2e.ae2expert.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import extracells.api.gas.IAEGasStack;
import extracells.api.gas.IGasStorageChannel;
import com.e2e.ae2expert.storage.CellTier;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.BasicCellInventoryHandler;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.PrecisePriorityList;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Ячейка с безопасными счётчиками long и фильтрами AE2. */
public final class ItemExpertGasCell extends ExpertStorageCell<IAEGasStack> {
    public ItemExpertGasCell(CellTier tier) { super("gas_cell_" + tier.getId(), tier); }
    @Override public IStorageChannel<IAEGasStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class);
    }

    @Override protected <S extends IAEStack<S>> BasicCellInventoryHandler<S> configureHandler(
            BasicCellInventoryHandler<S> handler, ItemStack stack, IStorageChannel<S> channel) {
        // Канал ExtraCells 2.6.7 не принимает свой ItemGas в createStack(Object).
        IItemList<S> filter = channel.createList();
        IItemHandler config = getConfigInventory(stack);
        for (int slot = 0; slot < config.getSlots(); slot++) {
            ItemStack sample = config.getStackInSlot(slot);
            if (sample.isEmpty()) continue;
            S key;
            if (sample.getItem() == extracells.item.ItemGas$.MODULE$) {
                mekanism.api.gas.Gas gas = mekanism.api.gas.GasRegistry.getGas(extracells.item.ItemGas$.MODULE$.getGasName(sample));
                key = gas == null ? null : channel.createStack(gas);
            } else key = channel.createStack(sample);
            if (key != null) filter.add(key);
        }
        if (!filter.isEmpty()) {
            boolean fuzzy = false;
            IItemHandler upgrades = getUpgradesInventory(stack);
            for (int slot = 0; slot < upgrades.getSlots(); slot++) {
                ItemStack card = upgrades.getStackInSlot(slot);
                if (card.getItem() instanceof IUpgradeModule
                        && ((IUpgradeModule) card.getItem()).getType(card) == Upgrades.FUZZY) fuzzy = true;
            }
            handler.setPartitionList(fuzzy ? new FuzzyPriorityList<>(filter, getFuzzyMode(stack)) : new PrecisePriorityList<>(filter));
        }
        return handler;
    }
}

package com.e2e.ae2expert.storage;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import com.e2e.ae2expert.item.ExpertStorageCell;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandler;

/** Счётчики long для всех каналов; формат NBT не зависит от ограничений BasicCellInventory. */
public class LongCellInventory<T extends IAEStack<T>> implements ICellInventory<T> {
    private final ItemStack cell;
    private final ISaveProvider saveProvider;
    private final IStorageChannel<T> channel;
    private final long totalBytes;
    private final int maxTypes, bytesPerType;
    private final double drain;
    private IItemList<T> items;
    private long storedUnits;
    private int storedTypes;
    // Неразбираемые записи сохраняются без изменений, а не исчезают при следующей записи NBT.
    private NBTTagList quarantined = new NBTTagList();

    public LongCellInventory(ItemStack cell, ISaveProvider saveProvider, IStorageChannel<T> channel,
            long totalBytes, int maxTypes, int bytesPerType, double drain) {
        this.cell = cell;
        this.saveProvider = saveProvider;
        this.channel = channel;
        this.totalBytes = totalBytes;
        this.maxTypes = maxTypes;
        this.bytesPerType = bytesPerType;
        this.drain = drain;
        items = channel.createList();
        NBTTagCompound root = cell.getTagCompound();
        if (root == null) return;
        quarantined = root.getTagList("expertInvalidItems", 10).copy();
        NBTTagList data = root.getTagList("expertItems", 10);
        for (int i = 0; i < data.tagCount(); i++) {
            NBTTagCompound raw = data.getCompoundTagAt(i);
            try {
                T entry = channel.createFromNBT(raw);
                if (entry == null || entry.getStackSize() <= 0
                        || entry.getStackSize() > Long.MAX_VALUE - storedUnits) {
                    quarantined.appendTag(raw.copy());
                    continue;
                }
                if (items.findPrecise(entry) == null) storedTypes++;
                storedUnits += entry.getStackSize();
                items.addStorage(entry);
            } catch (RuntimeException malformed) {
                quarantined.appendTag(raw.copy());
            }
        }
    }

    public boolean hasInvalidEntries() { return quarantined.tagCount() > 0; }

    private void changed() {
        if (saveProvider == null) persist(); else saveProvider.saveChanges(this);
    }
    @Override public ItemStack getItemStack() { return cell; }
    @Override public IStorageChannel<T> getChannel() { return channel; }
    @Override public double getIdleDrain() { return drain; }
    @Override public FuzzyMode getFuzzyMode() { return ((ICellWorkbenchItem) cell.getItem()).getFuzzyMode(cell); }
    @Override public IItemHandler getConfigInventory() { return ((ICellWorkbenchItem) cell.getItem()).getConfigInventory(cell); }
    @Override public IItemHandler getUpgradesInventory() { return ((ICellWorkbenchItem) cell.getItem()).getUpgradesInventory(cell); }
    @Override public int getBytesPerType() { return bytesPerType; }
    @Override public long getTotalBytes() { return totalBytes; }
    @Override public long getTotalItemTypes() { return maxTypes; }
    @Override public long getStoredItemTypes() {
        return storedTypes;
    }
    @Override public long getStoredItemCount() {
        return storedUnits;
    }
    @Override public long getUsedBytes() {
        long count = getStoredItemCount(), units = channel.getUnitsPerByte();
        long payload = count / units + (count % units == 0 ? 0 : 1);
        long overhead = getStoredItemTypes() * bytesPerType;
        return payload > Long.MAX_VALUE - overhead ? Long.MAX_VALUE : payload + overhead;
    }
    @Override public long getFreeBytes() { return Math.max(0, totalBytes - getUsedBytes()); }
    @Override public long getRemainingItemCount() {
        long bytes = Math.max(0, totalBytes - getStoredItemTypes() * bytesPerType);
        long units = channel.getUnitsPerByte();
        long capacity = bytes > Long.MAX_VALUE / units ? Long.MAX_VALUE : bytes * units;
        return Math.max(0, capacity - getStoredItemCount());
    }
    @Override public int getUnusedItemCount() {
        long remainder = getStoredItemCount() % channel.getUnitsPerByte();
        return remainder == 0 ? 0 : (int) (channel.getUnitsPerByte() - remainder);
    }
    @Override public long getRemainingItemTypes() {
        return Math.max(0, Math.min(maxTypes - getStoredItemTypes(), getFreeBytes() / bytesPerType));
    }
    @Override public boolean canHoldNewItem() {
        return getStoredItemTypes() < maxTypes && getRemainingItemCount() > (long) bytesPerType * channel.getUnitsPerByte();
    }
    @Override public int getStatusForCell() {
        return canHoldNewItem() ? 1 : getRemainingItemCount() > 0 ? 2 : 3;
    }

    @Override public T injectItems(T input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) return null;
        // Исключаем рекурсивное вложение ячеек с NBT и неограниченный рост сетевых пакетов.
        if (input instanceof IAEItemStack) {
            ItemStack definition = ((IAEItemStack) input).getDefinition();
            if (definition.getItem() instanceof ExpertStorageCell
                    || definition.getItem() instanceof appeng.api.implementations.items.IStorageCell) return input;
        }
        T existing = items.findPrecise(input);
        boolean newType = existing == null || existing.getStackSize() <= 0;
        if (newType && !canHoldNewItem()) return input;
        long overhead = newType ? (long) bytesPerType * channel.getUnitsPerByte() : 0;
        long accepted = Math.min(input.getStackSize(), Math.max(0, getRemainingItemCount() - overhead));
        if (accepted <= 0) return input;
        if (mode == Actionable.MODULATE) {
            T added = input.copy();
            added.setStackSize(accepted);
            items.addStorage(added);
            storedUnits += accepted;
            if (newType) storedTypes++;
            changed();
        }
        if (accepted == input.getStackSize()) return null;
        T remainder = input.copy();
        remainder.setStackSize(input.getStackSize() - accepted);
        return remainder;
    }
    @Override public T extractItems(T request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) return null;
        T existing = items.findPrecise(request);
        if (existing == null || existing.getStackSize() <= 0) return null;
        T result = request.copy();
        result.setStackSize(Math.min(request.getStackSize(), existing.getStackSize()));
        if (mode == Actionable.MODULATE) {
            existing.setStackSize(existing.getStackSize() - result.getStackSize());
            storedUnits -= result.getStackSize();
            if (existing.getStackSize() == 0) {
                storedTypes--;
                IItemList<T> compact = channel.createList();
                for (T stack : items) if (stack.getStackSize() > 0) compact.addStorage(stack);
                items = compact;
            }
            changed();
        }
        return result;
    }
    @Override public IItemList<T> getAvailableItems(IItemList<T> result) {
        for (T stack : items) if (stack.getStackSize() > 0) result.addStorage(stack.copy());
        return result;
    }
    @Override public void persist() {
        NBTTagCompound root = cell.getTagCompound();
        if (root == null) { root = new NBTTagCompound(); cell.setTagCompound(root); }
        NBTTagList data = new NBTTagList();
        for (T stack : items) if (stack.getStackSize() > 0) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            data.appendTag(tag);
        }
        root.setTag("expertItems", data);
        if (quarantined.tagCount() > 0) root.setTag("expertInvalidItems", quarantined.copy());
        else root.removeTag("expertInvalidItems");
    }
}

package com.e2e.ae2expert.interfacepkg;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class ContainerExpertInterface extends Container {
    public final TileExpertInterface tile;
    public int tab;
    public boolean active;
    private final int size;

    public ContainerExpertInterface(EntityPlayer player, TileExpertInterface tile) {
        this.tile = tile;
        size = tile.getSize();
        ItemStackHandler[] groups = {tile.config, tile.buffer, tile.patterns};
        for (int group = 0; group < 3; group++) {
            final int page = group;
            for (int slot = 0; slot < size; slot++) {
                addSlotToContainer(new SlotItemHandler(groups[group], slot, 50 + slot % 8 * 18, 32 + slot / 8 * 18) {
                    @Override public boolean isEnabled() { return tab == page; }
                    @Override public boolean canTakeStack(EntityPlayer player) { return page != 0 && tab == page; }
                    @Override public boolean isItemValid(ItemStack stack) { return page != 0 && tab == page && super.isItemValid(stack); }
                });
            }
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player.inventory, col + row * 9 + 9, 42 + col * 18, 180 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col, 42 + col * 18, 238));
        setTab(0);
    }

    public void setTab(int value) {
        if (value < 0 || value > 2) return;
        tab = value;
        for (int group = 0; group < 3; group++) for (int slot = 0; slot < size; slot++) {
            Slot current = inventorySlots.get(group * size + slot);
            // На сервере тоже убираем невидимые слоты из области обычных кликов.
            current.xPos = group == value ? 50 + slot % 8 * 18 : -10000;
        }
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return tile.canUse(player); }
    @Override public boolean enchantItem(EntityPlayer player, int action) {
        if (!canInteractWith(player) || action < 0 || action > 2) return false;
        setTab(action);
        return true;
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        active = tile.getProxy().isActive();
        for (IContainerListener listener : listeners) {
            listener.sendWindowProperty(this, 0, tab);
            listener.sendWindowProperty(this, 1, active ? 1 : 0);
        }
    }
    @Override public void updateProgressBar(int id, int value) {
        if (id == 0) setTab(value);
        if (id == 1) active = value != 0;
    }

    @Override public ItemStack slotClick(int id, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player)) return ItemStack.EMPTY;
        if (id >= 0 && id < size * 3) {
            if (id / size != tab) return ItemStack.EMPTY;
            if (tab == 0) {
                if (type != ClickType.PICKUP || button > 1) return ItemStack.EMPTY;
                ItemStack cursor = player.inventory.getItemStack();
                ItemStack old = tile.config.getStackInSlot(id);
                ItemStack ghost = cursor.isEmpty() ? ItemStack.EMPTY : cursor.copy();
                if (!ghost.isEmpty()) ghost.setCount(button == 1 ? 1 : Math.min(cursor.getCount(), ghost.getMaxStackSize()));
                if (cursor.isEmpty() && button == 1 && !old.isEmpty()) {
                    ghost = old.copy();
                    ghost.shrink(1);
                }
                tile.config.setStackInSlot(id, ghost);
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(id, button, type, player);
    }

    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.slotNumber >= size && super.canMergeSlot(stack, slot);
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!canInteractWith(player)) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || (index < size * 3 && index / size != tab)) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        if (index < size) return ItemStack.EMPTY;
        if (index < size * 3) {
            if (!mergeItemStack(stack, size * 3, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (tab == 0 || !mergeItemStack(stack, tab * size, (tab + 1) * size, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}

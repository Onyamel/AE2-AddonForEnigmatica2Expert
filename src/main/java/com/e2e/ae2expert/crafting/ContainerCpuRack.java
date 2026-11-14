package com.e2e.ae2expert.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class ContainerCpuRack extends Container {
    private final TileCpuRack rack;
    public boolean locked;
    private boolean lastSentLocked;
    public long bytes;
    public int processors;

    public ContainerCpuRack(EntityPlayer player, TileCpuRack rack) {
        this.rack = rack;
        for (int i = 0; i < TileCpuRack.SLOTS; i++) {
            addSlotToContainer(new SlotItemHandler(rack.modules, i, 8 + i % 9 * 18, 49 + i / 9 * 18) {
                @Override public boolean canTakeStack(EntityPlayer player) { return !locked; }
                @Override public boolean isItemValid(ItemStack stack) { return !locked && super.isItemValid(stack); }
            });
        }
        for (int i = 0; i < 27; i++) addSlotToContainer(new Slot(player.inventory, i + 9, 8 + i % 9 * 18, 101 + i / 9 * 18));
        for (int i = 0; i < 9; i++) addSlotToContainer(new Slot(player.inventory, i, 8 + i * 18, 159));
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return rack.canUse(player); }

    @Override
    public ItemStack slotClick(int slot, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player) || slot >= inventorySlots.size() || (slot < 0 && slot != -1 && slot != -999))
            return ItemStack.EMPTY;
        if (!player.world.isRemote) locked = !rack.mayEdit();
        if (locked && slot >= 0 && slot < TileCpuRack.SLOTS) {
            resetDrag();
            return ItemStack.EMPTY;
        }
        ItemStack result = super.slotClick(slot, button, type, player);
        if (!player.world.isRemote && !locked) rack.updateModules();
        return result;
    }

    @Override public boolean canDragIntoSlot(Slot slot) { return !locked || slot.slotNumber >= TileCpuRack.SLOTS; }
    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) { return !locked || slot.slotNumber >= TileCpuRack.SLOTS; }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!canInteractWith(player) || locked || index < 0 || index >= inventorySlots.size()
                || (!player.world.isRemote && !rack.mayEdit())) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < TileCpuRack.SLOTS) {
            if (!mergeItemStack(stack, TileCpuRack.SLOTS, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else if (!CpuModule.accepts(stack) || !mergeItemStack(stack, 0, TileCpuRack.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, stack);
        if (!player.world.isRemote) rack.updateModules();
        return original;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendStats(listener);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        long nextBytes = rack.craftingBytes();
        int nextProcessors = rack.coprocessors();
        boolean nextLocked = !rack.mayEdit();
        if (nextBytes == bytes && nextProcessors == processors && nextLocked == lastSentLocked) return;
        bytes = nextBytes;
        processors = nextProcessors;
        locked = nextLocked;
        lastSentLocked = nextLocked;
        for (IContainerListener listener : listeners) sendStats(listener);
    }

    private void sendStats(IContainerListener listener) {
        // Vanilla window properties carry signed shorts. Preserve every bit of each number.
        for (int word = 0; word < 4; word++) listener.sendWindowProperty(this, word, (int) (bytes >>> (16 * word)) & 0xffff);
        listener.sendWindowProperty(this, 4, processors & 0xffff);
        listener.sendWindowProperty(this, 5, processors >>> 16);
        listener.sendWindowProperty(this, 6, locked ? 1 : 0);
    }

    @Override
    public void updateProgressBar(int id, int value) {
        int word = value & 0xffff;
        if (id >= 0 && id < 4) {
            int shift = id * 16;
            bytes = bytes & ~(0xffffL << shift) | (long) word << shift;
        } else if (id == 4) processors = processors & 0xffff0000 | word;
        else if (id == 5) processors = processors & 0xffff | word << 16;
        else if (id == 6) locked = value != 0;
    }
}

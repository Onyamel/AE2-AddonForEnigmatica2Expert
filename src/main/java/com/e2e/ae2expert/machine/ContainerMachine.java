package com.e2e.ae2expert.machine;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.SlotItemHandler;

public final class ContainerMachine extends Container {
    public final TileEntity tile;
    public int energy, progress, eu;
    public boolean rfToEu;
    private final int machineSlots;

    public ContainerMachine(EntityPlayer player, TileEntity tile) {
        this.tile = tile;
        machineSlots = tile instanceof TileFluixChamber ? 4 : 0;
        if (machineSlots > 0) {
            for (int i = 0; i < 4; i++) {
                final int slot = i;
                addSlotToContainer(new SlotItemHandler(((TileFluixChamber) tile).getInventory(), i, 26 + i * 36, 42) {
                    @Override public boolean isItemValid(ItemStack stack) {
                        return slot != TileFluixChamber.SLOT_OUTPUT && super.isItemValid(stack);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player.inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 142));
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return tile.getWorld() != null && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override public boolean enchantItem(EntityPlayer player, int action) {
        if (action == 0 && canInteractWith(player) && tile instanceof TileRfEuConverter) {
            ((TileRfEuConverter) tile).toggleMode();
            return true;
        }
        return false;
    }

    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (tile instanceof TileFluixChamber) {
            energy = ((TileFluixChamber) tile).getEnergyStored();
            progress = ((TileFluixChamber) tile).getProgress();
        } else {
            energy = ((TileRfEuConverter) tile).getEnergyStored();
            eu = (int) ((TileRfEuConverter) tile).getEuBuffer();
            rfToEu = ((TileRfEuConverter) tile).isRfToEu();
        }
        for (IContainerListener listener : listeners) {
            listener.sendWindowProperty(this, 0, energy & 0xffff);
            listener.sendWindowProperty(this, 1, energy >>> 16);
            listener.sendWindowProperty(this, 2, progress);
            listener.sendWindowProperty(this, 3, eu);
            listener.sendWindowProperty(this, 4, rfToEu ? 1 : 0);
        }
    }

    @Override public void updateProgressBar(int id, int value) {
        if (id == 0) energy = (energy & 0xffff0000) | (value & 0xffff);
        if (id == 1) energy = (energy & 0xffff) | ((value & 0xffff) << 16);
        if (id == 2) progress = value;
        if (id == 3) eu = value;
        if (id == 4) rfToEu = value != 0;
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        if (index < machineSlots) {
            if (!mergeItemStack(stack, machineSlots, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else if (machineSlots > 0) {
            if (!mergeItemStack(stack, 0, machineSlots - 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}

package com.e2e.ae2expert.machine;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Fluix crystal chamber: turns one charged certus quartz, one redstone and one nether quartz into a
 * two fluix crystals, powered by RF.
 *
 * <p>This is the addon's alternative to AE2's in world crystal growing, which is painful to automate
 * in an expert pack.</p>
 */
public class TileFluixChamber extends TileEntity implements ITickable, cofh.redstoneflux.api.IEnergyReceiver {

    public static final int SLOT_CERTUS = 0;
    public static final int SLOT_REDSTONE = 1;
    public static final int SLOT_QUARTZ = 2;
    public static final int SLOT_OUTPUT = 3;

    private static final int ENERGY_CAPACITY = 20000;
    private static final int ENERGY_PER_TICK = FluixChamberRecipe.RF_PER_TICK;
    private static final int PROGRESS_MAX = FluixChamberRecipe.DURATION;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            TileFluixChamber.this.markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < SLOT_OUTPUT
                    && (isChargedCertus(stack) || isRedstone(stack) || isNetherQuartz(stack));
        }
    };

    private final MachineEnergy energy = new MachineEnergy(ENERGY_CAPACITY, 1000, 0, this::markDirty);

    // GUI и сама машина используют исходный инвентарь; трубы извлекают только результат.
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= 0 && slot < SLOT_OUTPUT && inventory.getStackInSlot(slot).isEmpty()) {
                int empty = 0;
                int missing = 0;
                for (int i = 0; i < SLOT_OUTPUT; i++) {
                    if (inventory.getStackInSlot(i).isEmpty()) empty++;
                    if (!FluixChamberRecipe.accepts(i, stack) && findIngredient(i) < 0) missing++;
                }
                if (empty <= missing) return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return inventory.isItemValid(slot, stack); }
    };

    private int progress;

    public int getProgress() {
        return this.progress;
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return ENERGY_CAPACITY;
    }

    @Override public boolean canConnectEnergy(EnumFacing side) { return true; }
    @Override public int receiveEnergy(EnumFacing side, int amount, boolean simulate) { return energy.receiveEnergy(amount, simulate); }
    @Override public int getEnergyStored(EnumFacing side) { return energy.getEnergyStored(); }
    @Override public int getMaxEnergyStored(EnumFacing side) { return ENERGY_CAPACITY; }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (!this.canCraft()) {
            if (this.progress != 0) { this.progress = 0; this.markDirty(); }
            return;
        }
        if (this.energy.getEnergyStored() < ENERGY_PER_TICK) {
            return;
        }

        this.energy.consume(ENERGY_PER_TICK);
        this.progress++;
        if (this.progress >= PROGRESS_MAX) {
            this.progress = 0;
            this.craft();
        }
        this.markDirty();
    }

    private int findIngredient(int ingredient) {
        for (int slot = 0; slot < SLOT_OUTPUT; slot++) {
            if (FluixChamberRecipe.accepts(ingredient, inventory.getStackInSlot(slot))) return slot;
        }
        return -1;
    }

    private boolean canCraft() {
        for (int ingredient = 0; ingredient < SLOT_OUTPUT; ingredient++) {
            if (findIngredient(ingredient) < 0) return false;
        }
        if (ModMachineItems.fluixCrystal().isEmpty()) return false;
        ItemStack output = this.inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        ItemStack result = ModMachineItems.fluixCrystal();
        return ItemStack.areItemsEqual(output, result) && ItemStack.areItemStackTagsEqual(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft() {
        for (int ingredient = 0; ingredient < SLOT_OUTPUT; ingredient++) {
            inventory.extractItem(findIngredient(ingredient), 1, false);
        }

        ItemStack result = ModMachineItems.fluixCrystal();
        ItemStack output = this.inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
            this.inventory.setStackInSlot(SLOT_OUTPUT, output);
        }
    }

    // ------------------------------------------------------------------ item matching

    public static boolean isChargedCertus(ItemStack stack) {
        return FluixChamberRecipe.accepts(SLOT_CERTUS, stack);
    }

    public static boolean isRedstone(ItemStack stack) {
        return FluixChamberRecipe.accepts(SLOT_REDSTONE, stack);
    }

    public static boolean isNetherQuartz(ItemStack stack) {
        return FluixChamberRecipe.accepts(SLOT_QUARTZ, stack);
    }

    // ------------------------------------------------------------------ capabilities

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.automation);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this.energy);
        }
        return super.getCapability(capability, facing);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("inventory", this.inventory.serializeNBT());
        compound.setInteger("energy", this.energy.getEnergyStored());
        compound.setInteger("progress", this.progress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.inventory.deserializeNBT(compound.getCompoundTag("inventory"));
        this.progress = Math.max(0, Math.min(PROGRESS_MAX - 1, compound.getInteger("progress")));
        this.energy.load(compound.getInteger("energy"));
    }

    /** Drops the contents so that breaking the block never eats items. */
    public void dropContents(EntityPlayer player) {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(this.world, this.pos.getX(), this.pos.getY(),
                        this.pos.getZ(), stack);
                this.inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}

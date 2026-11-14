package com.e2e.ae2expert.machine;

import net.minecraftforge.energy.EnergyStorage;

/** Внутренние операции отделены от внешних ограничений capability. */
public class MachineEnergy extends EnergyStorage {
    private final Runnable changed;

    public MachineEnergy(int capacity, int receive, int extract, Runnable changed) {
        super(capacity, receive, extract);
        this.changed = changed;
    }

    public void load(int value) { this.energy = Math.max(0, Math.min(this.capacity, value)); }
    public int consume(int amount) {
        int taken = Math.min(Math.max(0, amount), this.energy);
        this.energy -= taken;
        if (taken > 0) this.changed.run();
        return taken;
    }
    public int store(int amount) {
        int accepted = Math.min(Math.max(0, amount), this.capacity - this.energy);
        this.energy += accepted;
        if (accepted > 0) this.changed.run();
        return accepted;
    }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        int result = super.receiveEnergy(Math.max(0, amount), simulate);
        if (!simulate && result > 0) this.changed.run();
        return result;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        int result = super.extractEnergy(Math.max(0, amount), simulate);
        if (!simulate && result > 0) this.changed.run();
        return result;
    }
}

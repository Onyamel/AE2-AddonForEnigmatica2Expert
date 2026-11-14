package com.e2e.ae2expert.machine;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;

/**
 * RF &lt;-&gt; EU converter with a player selectable direction.
 *
 * <p>The exchange rate is the community standard of 4 RF per EU, with a 10% conversion loss applied
 * on both directions, so the machine can never be used to generate free energy.</p>
 */
public class TileRfEuConverter extends TileEntity implements ITickable, IEnergySink, IEnergySource,
        cofh.redstoneflux.api.IEnergyReceiver, cofh.redstoneflux.api.IEnergyProvider {

    private static final int MAX_RF = 40000;
    private static final double MAX_EU = 10000.0D;
    private static final double RF_PER_EU = 4.0D;
    private static final double LOSS = 0.10D;
    private static final double MAX_EU_PER_TICK = 256.0D;

    private final MachineEnergy rf = new MachineEnergy(MAX_RF, 4096, 4096, this::markDirty) {
        @Override public boolean canReceive() { return rfToEu; }
        @Override public boolean canExtract() { return !rfToEu; }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            return canReceive() ? super.receiveEnergy(amount, simulate) : 0;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            return canExtract() ? super.extractEnergy(amount, simulate) : 0;
        }
    };

    private double euBuffer;
    private boolean rfToEu = true;
    private boolean registeredWithIc2;

    public boolean isRfToEu() {
        return this.rfToEu;
    }

    public void toggleMode() {
        if (this.world != null && this.world.isRemote) return;
        this.unregisterFromIc2();
        this.rfToEu = !this.rfToEu;
        this.markDirty();
        this.onLoad();
        if (this.world != null) {
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos),
                    this.world.getBlockState(this.pos), 3);
        }
    }

    public int getEnergyStored() {
        return this.rf.getEnergyStored();
    }

    public double getEuBuffer() {
        return this.euBuffer;
    }

    @Override public boolean canConnectEnergy(EnumFacing side) { return true; }
    @Override public int receiveEnergy(EnumFacing side, int amount, boolean simulate) { return rf.receiveEnergy(amount, simulate); }
    @Override public int extractEnergy(EnumFacing side, int amount, boolean simulate) { return rf.extractEnergy(amount, simulate); }
    @Override public int getEnergyStored(EnumFacing side) { return rf.getEnergyStored(); }
    @Override public int getMaxEnergyStored(EnumFacing side) { return MAX_RF; }

    // ------------------------------------------------------------------ IC2 registration

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.world != null && !this.world.isRemote && !this.registeredWithIc2) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            this.registeredWithIc2 = true;
        }
    }

    @Override
    public void invalidate() {
        this.unregisterFromIc2();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.unregisterFromIc2();
        super.onChunkUnload();
    }

    private void unregisterFromIc2() {
        if (this.world != null && !this.world.isRemote && this.registeredWithIc2) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.registeredWithIc2 = false;
        }
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (this.rfToEu) {
            this.convertRfToEu();
        } else {
            this.convertEuToRf();
            for (EnumFacing side : EnumFacing.values()) {
                if (!this.world.isBlockLoaded(this.pos.offset(side))) continue;
                TileEntity target = this.world.getTileEntity(this.pos.offset(side));
                if (target == null) continue;
                net.minecraftforge.energy.IEnergyStorage sink = target.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
                if (sink != null && sink.canReceive()) {
                    int offered = Math.min(4096, this.rf.getEnergyStored());
                    int accepted = sink.receiveEnergy(offered, false);
                    this.rf.consume(Math.max(0, Math.min(offered, accepted)));
                }
            }
        }
    }

    private void convertRfToEu() {
        if (this.euBuffer >= MAX_EU) {
            return;
        }
        double roomEu = Math.min(MAX_EU - this.euBuffer, MAX_EU_PER_TICK);
        int neededRf = (int) Math.ceil(roomEu * RF_PER_EU);
        int available = Math.min(neededRf, this.rf.getEnergyStored());
        if (available <= 0) {
            return;
        }
        double eu = (available / RF_PER_EU) * (1.0D - LOSS);
        this.rf.consume(available);
        this.euBuffer = Math.min(MAX_EU, this.euBuffer + eu);
        this.markDirty();
    }

    private void convertEuToRf() {
        if (this.euBuffer <= 0.0D || this.rf.getEnergyStored() >= MAX_RF) {
            return;
        }
        double euToUse = Math.min(this.euBuffer, MAX_EU_PER_TICK);
        int rfRoom = MAX_RF - this.rf.getEnergyStored();
        int rfToAdd = (int) Math.floor(euToUse * RF_PER_EU * (1.0D - LOSS));
        rfToAdd = Math.min(rfToAdd, rfRoom);
        if (rfToAdd <= 0) {
            return;
        }
        double euSpent = (rfToAdd / (RF_PER_EU * (1.0D - LOSS)));
        this.euBuffer = Math.max(0.0D, this.euBuffer - euSpent);
        this.rf.store(rfToAdd);
        this.markDirty();
    }

    // ------------------------------------------------------------------ IEnergySink

    @Override
    public double getDemandedEnergy() {
        return this.rfToEu ? 0.0D : Math.max(0.0D, MAX_EU - this.euBuffer);
    }

    @Override
    public int getSinkTier() {
        return 4;
    }

    @Override
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        if (this.rfToEu || amount <= 0.0D || !Double.isFinite(amount)) {
            return amount;
        }
        double accepted = Math.min(amount, Math.max(0.0D, MAX_EU - this.euBuffer));
        this.euBuffer += accepted;
        this.markDirty();
        return amount - accepted;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing direction) {
        return !this.rfToEu;
    }

    // ------------------------------------------------------------------ IEnergySource

    @Override
    public double getOfferedEnergy() {
        return this.rfToEu ? Math.min(MAX_EU_PER_TICK, this.euBuffer) : 0.0D;
    }

    @Override
    public void drawEnergy(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return;
        this.euBuffer = Math.max(0.0D, this.euBuffer - amount);
        this.markDirty();
    }

    @Override
    public int getSourceTier() {
        return 4;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing direction) {
        return this.rfToEu;
    }

    // ------------------------------------------------------------------ capabilities

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this.rf);
        }
        return super.getCapability(capability, facing);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("rf", this.rf.getEnergyStored());
        compound.setDouble("eu", this.euBuffer);
        compound.setBoolean("rfToEu", this.rfToEu);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.rf.load(compound.getInteger("rf"));
        double loadedEu = compound.getDouble("eu");
        this.euBuffer = Double.isFinite(loadedEu) ? Math.max(0, Math.min(MAX_EU, loadedEu)) : 0;
        this.rfToEu = !compound.hasKey("rfToEu") || compound.getBoolean("rfToEu");
    }
}

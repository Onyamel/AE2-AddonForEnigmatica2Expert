package com.e2e.ae2expert.crafting;

import java.util.List;
import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.me.GridAccessException;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public final class TileCpuRack extends TileExpertCpu {
    static final int SLOTS = 18;
    private long bytes;
    private int processors;
    final ItemStackHandler modules = new ItemStackHandler(SLOTS) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return CpuModule.accepts(stack);
        }
        @Override protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    @Override public long craftingBytes() { return bytes; }
    @Override public int coprocessors() { return processors; }

    public boolean canUse(EntityPlayer player) {
        if (world == null || world.getTileEntity(pos) != this || player.getDistanceSqToCenter(pos) > 64) return false;
        if (world.isRemote || !getProxy().isReady()) return true;
        try {
            return getProxy().getSecurity().hasPermission(player, SecurityPermissions.BUILD);
        } catch (GridAccessException disconnected) {
            return getCluster() == null;
        }
    }

    boolean mayEdit() {
        if (!(getCluster() instanceof CraftingCPUCluster)) return true;
        CraftingCPUCluster cpu = (CraftingCPUCluster) getCluster();
        return !cpu.isBusy() && cpu.getInventory().getAvailableItems(AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class).createList()).isEmpty();
    }

    void updateModules() {
        long nextBytes = 0;
        int nextProcessors = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = modules.getStackInSlot(slot);
            if (!CpuModule.accepts(stack) || stack.getItem() == Items.AIR) continue;
            int count = Math.min(64, stack.getCount());
            nextBytes += (long) CpuModule.bytes(stack) * count;
            nextProcessors += CpuModule.processors(stack) * count;
        }
        if (nextBytes == bytes && nextProcessors == processors) return;
        bytes = nextBytes;
        processors = nextProcessors;
        markDirty();
        if (world == null || world.isRemote || !getProxy().isReady()) return;
        if (getCluster() != null) getCluster().destroy();
        updateMultiBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("modules", modules.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagCompound data = tag.getCompoundTag("modules").copy();
        data.setInteger("Size", SLOTS);
        modules.deserializeNBT(data);
        updateModules();
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = modules.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
    }
}

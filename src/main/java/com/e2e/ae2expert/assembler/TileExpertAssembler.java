package com.e2e.ae2expert.assembler;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.container.ContainerNull;
import appeng.me.GridAccessException;
import appeng.tile.crafting.TileMolecularAssembler;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Ускоренный сборщик: ограниченная очередь позволяет использовать остаток тика после крафта.
 * Каждый внутренний шаг и расход энергии по-прежнему обслуживает оригинальный AE2.
 */
public abstract class TileExpertAssembler extends TileMolecularAssembler {
    private final Deque<PendingCraft> queued = new ArrayDeque<>();

    /** How many crafting ticks are simulated per server tick. */
    protected abstract int getSpeedMultiplier();

    public int getQueuedCraftCount() { return queued.size(); }

    public static boolean sleepUnlessQueued(ITickManager manager, IGridNode node) {
        Object machine = node == null ? null : node.getMachine();
        if (machine instanceof TileExpertAssembler && ((TileExpertAssembler) machine).getQueuedCraftCount() > 0) {
            return false;
        }
        return manager.sleepDevice(node);
    }

    @Override public boolean pushPattern(ICraftingPatternDetails recipe, InventoryCrafting table, EnumFacing side) {
        if (recipe == null || !recipe.isCraftable() || !super.acceptsPlans()
                || queued.size() >= getSpeedMultiplier()) return false;
        if (queued.isEmpty() && super.pushPattern(recipe, table, side)) return true;
        queued.addLast(new PendingCraft(recipe, table, side));
        saveChanges();
        try { getProxy().getTick().wakeDevice(getProxy().getNode()); }
        catch (GridAccessException disconnected) { }
        return true;
    }

    @Override public TickingRequest getTickingRequest(IGridNode node) {
        TickingRequest original = super.getTickingRequest(node);
        return queued.isEmpty() ? original : new TickingRequest(1, 1, false, false);
    }

    private void startNext() {
        if (queued.isEmpty() || world == null) return;
        PendingCraft next = queued.peekFirst();
        if (!(next.pattern.getItem() instanceof ICraftingPatternItem)) return;
        try { if (next.recipe == null) next.recipe = ((ICraftingPatternItem) next.pattern.getItem()).getPatternForItem(next.pattern, world); }
        catch (RuntimeException invalid) { return; }
        ICraftingPatternDetails recipe = next.recipe;
        if (recipe == null || !recipe.isCraftable()) return;
        InventoryCrafting table = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int slot = 0; slot < 9; slot++) table.setInventorySlotContents(slot, next.inputs[slot].copy());
        if (super.pushPattern(recipe, table, next.side)) {
            queued.removeFirst();
            saveChanges();
        }
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        TickRateModulation result = TickRateModulation.IDLE;
        int iterations = Math.max(1, this.getSpeedMultiplier());
        for (int i = 0; i < iterations; i++) {
            startNext();
            result = super.tickingRequest(node, i == 0 ? ticksSinceLastCall : 1);
            if (result == TickRateModulation.SLEEP) {
                if (queued.isEmpty()) return result;
                // Не засыпаем с принятой очередью, включая временно неразбираемый шаблон.
                result = TickRateModulation.IDLE;
            }
        }
        return queued.isEmpty() ? result : TickRateModulation.FASTER;
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList jobs = new NBTTagList();
        for (PendingCraft job : queued) jobs.appendTag(job.write());
        tag.setTag("expertCraftQueue", jobs);
        return tag;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        queued.clear();
        NBTTagList jobs = tag.getTagList("expertCraftQueue", 10);
        for (int i = 0; i < jobs.tagCount(); i++) queued.addLast(new PendingCraft(jobs.getCompoundTagAt(i)));
    }

    @Override public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (PendingCraft job : queued) for (ItemStack input : job.inputs)
            if (!input.isEmpty()) drops.add(input.copy());
    }

    private static final class PendingCraft {
        final ItemStack pattern;
        final ItemStack[] inputs = new ItemStack[9];
        final EnumFacing side;
        ICraftingPatternDetails recipe;

        PendingCraft(ICraftingPatternDetails recipe, InventoryCrafting table, EnumFacing side) {
            this.pattern = recipe.getPattern().copy();
            this.recipe = recipe;
            this.side = side;
            for (int slot = 0; slot < 9; slot++) inputs[slot] = table.getStackInSlot(slot).copy();
        }

        PendingCraft(NBTTagCompound tag) {
            pattern = new ItemStack(tag.getCompoundTag("pattern"));
            side = EnumFacing.byIndex(tag.getInteger("side"));
            NBTTagList contents = tag.getTagList("inputs", 10);
            for (int slot = 0; slot < 9; slot++) inputs[slot] = slot < contents.tagCount()
                    ? new ItemStack(contents.getCompoundTagAt(slot)) : ItemStack.EMPTY;
        }

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("pattern", pattern.writeToNBT(new NBTTagCompound()));
            tag.setInteger("side", side.ordinal());
            NBTTagList contents = new NBTTagList();
            for (ItemStack input : inputs) contents.appendTag(input.writeToNBT(new NBTTagCompound()));
            tag.setTag("inputs", contents);
            return tag;
        }
    }

    /** x2 speed. */
    public static class X2 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 2;
        }
    }

    /** x4 speed. */
    public static class X4 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 4;
        }
    }

    /** x8 speed. */
    public static class X8 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 8;
        }
    }

    /** x16 speed. */
    public static class X16 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 16;
        }
    }

    /** x32 speed. */
    public static class X32 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 32;
        }
    }
}

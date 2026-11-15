package com.e2e.ae2expert.extended;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.ContainerNull;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkTile;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public final class TileTableAssembler extends AENetworkTile implements ICraftingMachine, ITickable {
    private static final int MAX_QUEUED = 8;
    private static final double WORK_PER_TICK = 80;
    private static final double WORK_PER_CRAFT = 100;
    private final java.util.ArrayDeque<Job> queue = new java.util.ArrayDeque<>();
    private InventoryCrafting input;
    private ItemStack pattern = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private final java.util.ArrayDeque<ItemStack> returns = new java.util.ArrayDeque<>();
    private EnumFacing target = EnumFacing.DOWN;
    private double progress;

    public TileTableAssembler() {
        getProxy().setIdlePowerUsage(0);
    }

    @Override public boolean acceptsPlans() { return true; }

    @Override
    public boolean pushPattern(ICraftingPatternDetails plan, InventoryCrafting grid, EnumFacing side) {
        if (!(plan instanceof TablePatternDetails) || side == null || world == null || world.isRemote
                || queue.size() >= MAX_QUEUED) return false;
        if (!(getBlockType() instanceof BlockTableAssembler)) return false;
        int width = ((TablePatternDetails) plan).matrixWidth();
        if (width > ((BlockTableAssembler) getBlockType()).width || grid.getWidth() != width
                || grid.getHeight() != width || plan.getOutput(grid, world).isEmpty()) return false;
        InventoryCrafting ingredients = matrix(width);
        for (int i = 0; i < grid.getSizeInventory(); i++)
            ingredients.setInventorySlotContents(i, grid.getStackInSlot(i).copy());
        queue.addLast(new Job(ingredients, plan.getPattern(), side));
        if (input == null && output.isEmpty() && returns.isEmpty()) startNext();
        markDirty();
        return true;
    }

    private void startNext() {
        Job next = queue.pollFirst();
        if (next == null) return;
        input = next.grid;
        pattern = next.pattern;
        target = next.target;
        progress = 0;
        markDirty();
    }

    @Override
    public void update() {
        if (!(world instanceof WorldServer)) return;
        if (!output.isEmpty() || !returns.isEmpty()) {
            deliver();
            if (!output.isEmpty() || !returns.isEmpty()) return;
        }
        double workLeft = WORK_PER_TICK;
        while (workLeft > 0) {
            if (input == null) startNext();
            if (input == null || !(pattern.getItem() instanceof ItemTablePattern) || !pattern.hasTagCompound()) return;
            if (!getProxy().isActive()) return;
            if (progress < WORK_PER_CRAFT) {
                double drawn;
                try {
                    drawn = getProxy().getEnergy().extractAEPower(
                            Math.min(workLeft, WORK_PER_CRAFT - progress), Actionable.MODULATE, PowerMultiplier.CONFIG);
                } catch (GridAccessException disconnected) {
                    return;
                }
                if (drawn <= 0) return;
                progress += drawn;
                workLeft -= drawn;
                markDirty();
            }
            if (progress < WORK_PER_CRAFT || !finishCraft()) return;
            deliver();
            if (!output.isEmpty() || !returns.isEmpty()) return;
        }
    }

    private boolean finishCraft() {
        IRecipe recipe = TableRecipes.find(input, world);
        ItemStack expected = new ItemStack(pattern.getTagCompound().getCompoundTag("output"));
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(input);
        if (result.isEmpty() || !ItemStack.areItemStacksEqual(expected, result)) return false;

        // AE2's crafting CPU accounts for container items through the same helper.
        for (int i = 0; i < input.getSizeInventory(); i++) {
            ItemStack remainder = Platform.getContainerItem(input.getStackInSlot(i));
            if (!remainder.isEmpty()) returns.addLast(remainder.copy());
        }
        output = result.copy();
        InventoryCrafting completed = input;
        input = null;
        pattern = ItemStack.EMPTY;
        progress = 0;
        markDirty();
        FMLCommonHandler.instance().firePlayerCraftingEvent(Platform.getPlayer((WorldServer) world), result, completed);
        return true;
    }

    private void deliver() {
        BlockPos adjacent = pos.offset(target);
        if (!world.isBlockLoaded(adjacent)) return;
        TileEntity tile = world.getTileEntity(adjacent);
        InventoryAdaptor adaptor = tile == null ? null : InventoryAdaptor.getAdaptor(tile, target.getOpposite());
        if (adaptor == null) return;
        if (!output.isEmpty()) {
            output = adaptor.addItems(output);
            markDirty();
            if (!output.isEmpty()) return;
        }
        while (!returns.isEmpty()) {
            ItemStack remainder = adaptor.addItems(returns.peekFirst());
            returns.removeFirst();
            markDirty();
            if (!remainder.isEmpty()) {
                returns.addFirst(remainder);
                break;
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("target", target.getIndex());
        tag.setDouble("progress", progress);
        tag.setTag("pattern", pattern.writeToNBT(new NBTTagCompound()));
        tag.setTag("output", output.writeToNBT(new NBTTagCompound()));
        if (input != null) {
            tag.setInteger("width", input.getWidth());
            tag.setTag("grid", writeGrid(input));
        } else {
            tag.removeTag("width");
            tag.removeTag("grid");
        }
        NBTTagList remaining = new NBTTagList();
        for (ItemStack stack : returns) remaining.appendTag(stack.writeToNBT(new NBTTagCompound()));
        tag.setTag("returns", remaining);
        NBTTagList queued = new NBTTagList();
        for (Job job : queue) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("width", job.grid.getWidth());
            entry.setInteger("target", job.target.getIndex());
            entry.setTag("pattern", job.pattern.writeToNBT(new NBTTagCompound()));
            entry.setTag("grid", writeGrid(job.grid));
            queued.appendTag(entry);
        }
        tag.setTag("queued", queued);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        target = EnumFacing.byIndex(tag.getInteger("target"));
        double savedProgress = tag.getDouble("progress");
        progress = Double.isFinite(savedProgress) ? Math.max(0, Math.min(WORK_PER_CRAFT, savedProgress)) : 0;
        pattern = new ItemStack(tag.getCompoundTag("pattern"));
        output = new ItemStack(tag.getCompoundTag("output"));
        input = readGrid(tag);
        queue.clear();
        NBTTagList queued = tag.getTagList("queued", 10);
        for (int i = 0; i < queued.tagCount(); i++) {
            NBTTagCompound entry = queued.getCompoundTagAt(i);
            InventoryCrafting ingredients = readGrid(entry);
            if (ingredients != null) queue.addLast(new Job(ingredients,
                    new ItemStack(entry.getCompoundTag("pattern")),
                    EnumFacing.byIndex(entry.getInteger("target"))));
        }
        returns.clear();
        NBTTagList remaining = tag.getTagList("returns", 10);
        for (int i = 0; i < remaining.tagCount(); i++) {
            ItemStack stack = new ItemStack(remaining.getCompoundTagAt(i));
            if (!stack.isEmpty()) returns.addLast(stack);
        }
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        if (!output.isEmpty()) drops.add(output.copy());
        for (ItemStack stack : returns) drops.add(stack.copy());
        if (input != null) dropIngredients(input, drops);
        for (Job job : queue) dropIngredients(job.grid, drops);
    }

    private static void dropIngredients(InventoryCrafting grid, List<ItemStack> drops) {
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack stack = grid.getStackInSlot(i);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
    }

    private static NBTTagList writeGrid(InventoryCrafting grid) {
        NBTTagList slots = new NBTTagList();
        for (int i = 0; i < grid.getSizeInventory(); i++)
            slots.appendTag(grid.getStackInSlot(i).writeToNBT(new NBTTagCompound()));
        return slots;
    }

    private static InventoryCrafting readGrid(NBTTagCompound tag) {
        int width = tag.getInteger("width");
        if (width != 3 && width != 5 && width != 7 && width != 9) return null;
        InventoryCrafting grid = matrix(width);
        NBTTagList slots = tag.getTagList("grid", 10);
        for (int i = 0; i < Math.min(slots.tagCount(), grid.getSizeInventory()); i++)
            grid.setInventorySlotContents(i, new ItemStack(slots.getCompoundTagAt(i)));
        return grid;
    }

    private static final class Job {
        final InventoryCrafting grid;
        final ItemStack pattern;
        final EnumFacing target;

        Job(InventoryCrafting grid, ItemStack pattern, EnumFacing target) {
            this.grid = grid;
            this.pattern = pattern;
            this.target = target;
        }
    }

    private static InventoryCrafting matrix(int width) {
        return new InventoryCrafting(new ContainerNull(), width, width);
    }
}

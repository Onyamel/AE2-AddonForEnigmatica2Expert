package com.e2e.ae2expert.interfacepkg;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkTile;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/** Параметризованный интерфейс: запасы, конфигурация и шаблоны хранятся независимо. */
public abstract class TileExpertInterface extends AENetworkTile implements ITickable, ICraftingProvider {
    private final int size;
    public final ItemStackHandler config;
    public final ItemStackHandler buffer;
    public final ItemStackHandler patterns;
    private final List<ICraftingPatternDetails> recipes = new ArrayList<>();
    private final List<ItemStack> pending = new ArrayList<>();
    // Остаток пополнения собственных запасов не принадлежит соседней машине.
    private ItemStack recovery = ItemStack.EMPTY;
    private boolean recipesDirty = true, advertisedActive;
    private EnumFacing pendingSide = EnumFacing.NORTH;
    private int clock, cursor;

    protected TileExpertInterface(int size) {
        this.size = size;
        config = inventory(size, false);
        buffer = inventory(size, false);
        patterns = inventory(size, true);
        getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        getProxy().setIdlePowerUsage(1.0 + size / 18.0);
        getProxy().setValidSides(EnumSet.allOf(EnumFacing.class));
    }

    private ItemStackHandler inventory(int slots, boolean pattern) {
        return new ItemStackHandler(slots) {
            @Override protected void onContentsChanged(int slot) {
                markDirty();
                if (pattern) recipesDirty = true;
            }
            @Override public int getSlotLimit(int slot) { return pattern ? 1 : 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return !pattern || stack.getItem() instanceof ICraftingPatternItem;
            }
        };
    }

    public int getSize() { return size; }

    public boolean canUse(EntityPlayer player) {
        if (world == null || world.getTileEntity(pos) != this || player.getDistanceSq(pos) > 64) return false;
        try { return getProxy().getSecurity().hasPermission(player, SecurityPermissions.BUILD); }
        catch (GridAccessException disconnected) { return true; }
    }

    @Override public void gridChanged() { recipesDirty = true; }

    @Override public void update() {
        if (world == null || world.isRemote || ++clock % 5 != 0) return;
        boolean active = getProxy().isActive();
        if (recipesDirty || advertisedActive != active) {
            rebuildRecipes();
            try {
                appeng.api.networking.IGrid grid = getProxy().getGrid();
                recipesDirty = false;
                advertisedActive = active;
                grid.postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
                // Синхронный обработчик события может снова пометить шаблоны изменёнными.
            } catch (GridAccessException disconnected) { recipesDirty = true; return; }
        }
        if (!active) return;
        flushPending();
        if (!recovery.isEmpty()) {
            ItemStack rest = ItemHandlerHelper.insertItemStacked(buffer, recovery, false);
            if (rest.getCount() != recovery.getCount()) markDirty();
            recovery = rest;
            if (!recovery.isEmpty()) return;
        }
        try {
            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = getProxy().getStorage().getInventory(channel);
            // Ограничиваем работу одним проходом из восьми слотов за вызов.
            for (int n = 0; n < Math.min(8, size); n++) {
                int slot = cursor++ % size;
                ItemStack have = buffer.getStackInSlot(slot), want = config.getStackInSlot(slot);
                boolean same = !have.isEmpty() && !want.isEmpty() && ItemHandlerHelper.canItemStacksStack(have, want);
                int excess = have.isEmpty() ? 0 : same ? Math.max(0, have.getCount() - want.getCount()) : have.getCount();
                if (excess > 0) {
                    IAEItemStack offered = channel.createStack(have.copy());
                    offered.setStackSize(excess);
                    IAEItemStack rest = Platform.poweredInsert(getProxy().getEnergy(), network, offered, new MachineSource(this));
                    int accepted = excess - (rest == null ? 0 : (int) rest.getStackSize());
                    if (accepted > 0) buffer.extractItem(slot, accepted, false);
                }
                have = buffer.getStackInSlot(slot);
                if (want.isEmpty() || (!have.isEmpty() && !ItemHandlerHelper.canItemStacksStack(have, want))) continue;
                int needed = want.getCount() - have.getCount();
                if (needed <= 0) continue;
                ItemStack request = want.copy();
                request.setCount(needed);
                int room = needed - buffer.insertItem(slot, request, true).getCount();
                if (room <= 0) continue;
                IAEItemStack key = channel.createStack(request);
                key.setStackSize(room);
                IAEItemStack acquired = Platform.poweredExtraction(getProxy().getEnergy(), network, key, new MachineSource(this));
                if (acquired != null) {
                    // Слот не изменяется сторонними тиками во время синхронного вызова сети.
                    ItemStack rest = buffer.insertItem(slot, acquired.createItemStack(), false);
                    if (!rest.isEmpty()) {
                        // Нештатный отказ инвентаря: сохраняем предметы в очереди, не уничтожаем их.
                        recovery = rest;
                        markDirty();
                        break;
                    }
                }
            }
            if (cursor >= size * 1000) cursor %= size;
        } catch (GridAccessException disconnected) {
            // Сеть могла отключиться после смены состояния.
        }
    }

    private void rebuildRecipes() {
        recipes.clear();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = patterns.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ICraftingPatternItem)) continue;
            try {
                ICraftingPatternDetails details = ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, world);
                if (details != null) recipes.add(details);
            } catch (RuntimeException invalidPattern) {
                // Повреждённый шаблон не должен отключать остальные рецепты.
            }
        }
    }

    @Override public void provideCrafting(ICraftingProviderHelper helper) {
        if (recipesDirty && world != null && !world.isRemote) rebuildRecipes();
        if (getProxy().isActive()) for (ICraftingPatternDetails recipe : recipes) helper.addCraftingOption(this, recipe);
    }

    public int getQueuedItemCount() {
        long count = recovery.getCount();
        for (ItemStack stack : pending) count += stack.getCount();
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    @Override public boolean isBusy() { return !getProxy().isActive() || !pending.isEmpty(); }

    @Override public boolean pushPattern(ICraftingPatternDetails recipe, InventoryCrafting table) {
        if (isBusy() || !recipes.contains(recipe)) return false;
        for (EnumFacing side : EnumFacing.values()) {
            if (!world.isBlockLoaded(pos.offset(side))) continue;
            TileEntity target = world.getTileEntity(pos.offset(side));
            if (target == null || target instanceof TileExpertInterface || target instanceof appeng.helpers.IInterfaceHost) continue;
            if (target instanceof ICraftingMachine && ((ICraftingMachine) target).acceptsPlans()) {
                if (((ICraftingMachine) target).pushPattern(recipe, table, side.getOpposite())) return true;
                continue;
            }
            if (recipe.isCraftable()) continue;
            IItemHandler handler = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (handler == null || !canFit(handler, table)) continue;
            pendingSide = side;
            for (int i = 0; i < table.getSizeInventory(); i++) {
                if (!table.getStackInSlot(i).isEmpty()) pending.add(table.getStackInSlot(i).copy());
            }
            markDirty();
            flushPending();
            return true;
        }
        return false;
    }

    /** Общая симуляция учитывает конкуренцию ингредиентов за одни и те же слоты. */
    private boolean canFit(IItemHandler target, InventoryCrafting table) {
        int slots = target.getSlots();
        if (slots < 1 || slots > 4096) return false;
        ItemStack[] reserved = new ItemStack[slots];
        int[] accepted = new int[slots];
        for (int i = 0; i < slots; i++) reserved[i] = target.getStackInSlot(i).copy();
        for (int i = 0; i < table.getSizeInventory(); i++) {
            ItemStack input = table.getStackInSlot(i);
            if (input.isEmpty()) continue;
            int remaining = input.getCount();
            // Фактический insertItemStacked сначала дополняет стаки, затем занимает пустые слоты.
            for (int pass = 0; pass < 2 && remaining > 0; pass++) for (int slot = 0; slot < slots && remaining > 0; slot++) {
                ItemStack current = reserved[slot];
                if ((pass == 0) == current.isEmpty()) continue;
                if (!current.isEmpty() && !ItemHandlerHelper.canItemStacksStack(current, input)) continue;
                ItemStack probe = input.copy();
                // Большие слоты (например, хранилища) могут принимать несколько обычных стаков.
                // Реальное ограничение определяет симуляция самого приёмника.
                int capacity = target.getSlotLimit(slot);
                int requested = (int) Math.min(capacity, (long) accepted[slot] + remaining);
                probe.setCount(requested);
                int allowed = requested - target.insertItem(slot, probe, true).getCount() - accepted[slot];
                int count = Math.min(remaining, Math.max(0, allowed));
                if (count <= 0) continue;
                accepted[slot] += count;
                if (current.isEmpty()) { current = input.copy(); current.setCount(0); reserved[slot] = current; }
                current.grow(count);
                remaining -= count;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private void flushPending() {
        if (pending.isEmpty() || !world.isBlockLoaded(pos.offset(pendingSide))) return;
        TileEntity tile = world.getTileEntity(pos.offset(pendingSide));
        if (tile == null || tile instanceof TileExpertInterface || tile instanceof appeng.helpers.IInterfaceHost) return;
        IItemHandler target = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, pendingSide.getOpposite());
        if (target == null) return;
        for (int i = 0; i < pending.size();) {
            ItemStack before = pending.get(i);
            ItemStack rest = ItemHandlerHelper.insertItemStacked(target, before, false);
            if (rest.getCount() != before.getCount()) markDirty();
            if (rest.isEmpty()) pending.remove(i); else { pending.set(i, rest); i++; }
        }
    }

    @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(cap, side);
    }
    @Override public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(buffer) : super.getCapability(cap, side);
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("config", config.serializeNBT());
        tag.setTag("buffer", buffer.serializeNBT());
        tag.setTag("patterns", patterns.serializeNBT());
        tag.setTag("recovery", recovery.writeToNBT(new NBTTagCompound()));
        NBTTagList queue = new NBTTagList();
        for (ItemStack stack : pending) queue.appendTag(stack.writeToNBT(new NBTTagCompound()));
        tag.setTag("pending", queue);
        tag.setInteger("pendingSide", pendingSide.ordinal());
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        config.deserializeNBT(tag.getCompoundTag("config"));
        buffer.deserializeNBT(tag.getCompoundTag("buffer"));
        patterns.deserializeNBT(tag.getCompoundTag("patterns"));
        recovery = new ItemStack(tag.getCompoundTag("recovery"));
        pending.clear();
        NBTTagList queue = tag.getTagList("pending", 10);
        for (int i = 0; i < queue.tagCount(); i++) {
            ItemStack stack = new ItemStack(queue.getCompoundTagAt(i));
            if (!stack.isEmpty()) pending.add(stack);
        }
        pendingSide = EnumFacing.byIndex(tag.getInteger("pendingSide"));
        recipesDirty = true;
    }

    @Override public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        for (int i = 0; i < size; i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) drops.add(buffer.getStackInSlot(i).copy());
            if (!patterns.getStackInSlot(i).isEmpty()) drops.add(patterns.getStackInSlot(i).copy());
        }
        for (ItemStack stack : pending) drops.add(stack.copy());
        if (!recovery.isEmpty()) drops.add(recovery.copy());
    }

    public static final class S18 extends TileExpertInterface { public S18() { super(18); } }
    public static final class S27 extends TileExpertInterface { public S27() { super(27); } }
    public static final class S36 extends TileExpertInterface { public S36() { super(36); } }
    public static final class S48 extends TileExpertInterface { public S48() { super(48); } }
    public static final class S64 extends TileExpertInterface { public S64() { super(64); } }
}

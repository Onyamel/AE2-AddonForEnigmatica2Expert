package com.e2e.ae2expert;

import java.util.List;
import java.util.concurrent.Future;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IBlocks;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.tile.crafting.TileCraftingTile;
import appeng.tile.storage.TileDrive;
import com.e2e.ae2expert.crafting.*;
import com.e2e.ae2expert.interfacepkg.*;
import com.e2e.ae2expert.storage.CellTier;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class RuntimeIntegrationChecksCpu {
    private static final IBlocks AE2 = AEApi.instance().definitions().blocks();
    private static final BlockPos RACK = new BlockPos(3, 96, 5);
    private static final BlockPos PROCESSING = RACK.north();
    private static TileCpuRack rack;
    private static TileExpertInterface provider;
    private static CraftingCPUCluster cpu;
    private static Future<ICraftingJob> plan;
    private static ICraftingLink job;
    private static int passed;

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError("Crafting CPU: " + name);
        passed++;
        System.out.println("[AE2ExpertCPU] PASS " + name);
    }

    private static Block block(String suffix) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ae2expert", "crafting_" + suffix));
    }

    private static void place(WorldServer world, BlockPos pos, Block block) {
        world.getChunk(pos);
        world.setBlockState(pos, block.getDefaultState());
    }

    private static void shiftIn(EntityPlayerMP player, ContainerCpuRack gui, ItemStack input, String description) {
        ItemStack old = player.inventory.getStackInSlot(9);
        try {
            player.inventory.setInventorySlotContents(9, input);
            require(!gui.transferStackInSlot(player, 18).isEmpty() && player.inventory.getStackInSlot(9).isEmpty(), description);
        } finally {
            player.inventory.setInventorySlotContents(9, old);
        }
    }

    public static void setup(WorldServer world, EntityPlayerMP player) {
        for (int x = -2; x < 26; x++) for (int z = 2; z <= 9; z++)
            world.setBlockToAir(new BlockPos(x, 96, z));

        for (int tier = 128, x = 8; tier <= 524288; tier *= 2, x++) {
            BlockPos pos = new BlockPos(x, 96, 8);
            place(world, pos, block("storage_" + tier + "k"));
            TileExpertCpu tile = (TileExpertCpu) world.getTileEntity(pos);
            require(tile.craftingBytes() == tier * 1024L && tile.isStorage(), "memory tier " + tier + "k");
        }

        place(world, RACK, CpuRegistry.RACK);
        rack = (TileCpuRack) world.getTileEntity(RACK);
        double x = player.posX, y = player.posY, z = player.posZ;
        try {
            player.setPosition(RACK.getX() + .5, RACK.getY(), RACK.getZ() + .5);
            ContainerCpuRack gui = new ContainerCpuRack(player, rack);
            require(gui.inventorySlots.size() == 54, "18 module slots + 36 player slots");
            shiftIn(player, gui, new ItemStack(block("storage_524288k"), 64), "stack of 64 memory blocks");
            shiftIn(player, gui, AE2.craftingStorage1k().maybeStack(1).get(), "stock AE2 memory module");
            shiftIn(player, gui, new ItemStack(block("coprocessor_729"), 64), "stack of 64 expert processors");
            shiftIn(player, gui, AE2.craftingAccelerator().maybeStack(1).get(), "stock AE2 processor module");
            require(rack.craftingBytes() == (1L << 35) + 1024 && rack.coprocessors() == 46657, "mixed capacities above int ranges");
            ItemStack prior = player.inventory.getStackInSlot(9);
            player.inventory.setInventorySlotContents(9, new ItemStack(Items.STICK));
            require(gui.transferStackInSlot(player, 18).isEmpty(), "unrelated item rejected");
            player.inventory.setInventorySlotContents(9, prior);
            TileCpuRack copy = new TileCpuRack();
            copy.readFromNBT(rack.writeToNBT(new NBTTagCompound()));
            require(copy.craftingBytes() == rack.craftingBytes() && copy.coprocessors() == rack.coprocessors(), "NBT round trip restores capacities");
            require(rack.getStorageBytes() == Integer.MAX_VALUE, "legacy int getter preserves positive memory");
        } finally {
            player.setPosition(x, y, z);
        }
        place(world, RACK.east(), block("coprocessor_9"));
        place(world, RACK.east(2), block("coprocessor_81"));
        place(world, RACK.east(3), AE2.craftingAccelerator().maybeBlock().get());
        place(world, RACK.east(4), AE2.craftingStorage64k().maybeBlock().get());
        place(world, RACK.west(), AE2.energyCellCreative().maybeBlock().get());
        place(world, PROCESSING, InterfaceRegistry.BLOCKS.get(0));
        provider = (TileExpertInterface) world.getTileEntity(PROCESSING);
        place(world, PROCESSING.north(), Blocks.CHEST);
        place(world, PROCESSING.west(), AE2.drive().maybeBlock().get());
        TileDrive drive = (TileDrive) world.getTileEntity(PROCESSING.west());
        ItemStack cell = new ItemStack(ModItems.itemCells.get(CellTier.K16));
        IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        AEApi.instance().registries().cell().getCellInventory(cell, null, channel)
                .injectItems(channel.createStack(new ItemStack(Items.REDSTONE, 64)), Actionable.MODULATE, null);
        drive.getInternalInventory().insertItem(0, cell, false);
        ItemStack pattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        net.minecraft.nbt.NBTTagList in = new net.minecraft.nbt.NBTTagList(), out = new net.minecraft.nbt.NBTTagList();
        in.appendTag(new ItemStack(Items.REDSTONE).writeToNBT(new NBTTagCompound()));
        out.appendTag(new ItemStack(Items.DIAMOND).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", in); tag.setTag("out", out); tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        provider.patterns.setStackInSlot(0, pattern);
    }

    private static void exerciseRackLimits(WorldServer world, EntityPlayerMP player) {
        BlockPos pos = new BlockPos(3, 96, 12);
        world.getChunk(pos);
        world.setBlockToAir(pos);
        place(world, pos, CpuRegistry.RACK);
        TileCpuRack standalone = (TileCpuRack) world.getTileEntity(pos);
        double x = player.posX, y = player.posY, z = player.posZ;
        ItemStack prior = player.inventory.getStackInSlot(9);
        try {
            player.setPosition(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            ContainerCpuRack gui = new ContainerCpuRack(player, standalone);
            for (int i = 0; i < 18; i++) {
                player.inventory.setInventorySlotContents(9, new ItemStack(block("storage_524288k"), 64));
                if (gui.transferStackInSlot(player, 18).isEmpty()) throw new AssertionError("rack slot " + i);
            }
            require(standalone.craftingBytes() == 576L * 1024 * 1024 * 1024, "full rack capacity is 576 GiB");
            require(gui.transferStackInSlot(player, 18).isEmpty(), "empty player slot does not move");
            player.inventory.setInventorySlotContents(9, new ItemStack(block("storage_524288k")));
            require(gui.transferStackInSlot(player, 18).isEmpty() && player.inventory.getStackInSlot(9).getCount() == 1,
                    "full rack leaves extra module untouched");
            long bytes = standalone.craftingBytes();
            for (int word = 0; word < 4; word++) gui.updateProgressBar(word, (short) (bytes >>> (16 * word)));
            gui.updateProgressBar(4, (short) 839808);
            gui.updateProgressBar(5, (short) (839808 >>> 16));
            require(gui.bytes == bytes && gui.processors == 839808, "signed GUI words preserve full memory and processor counts");
            java.util.ArrayList<ItemStack> drops = new java.util.ArrayList<>();
            standalone.getDrops(world, pos, drops);
            require(drops.size() == 18 && drops.stream().mapToInt(ItemStack::getCount).sum() == 1152,
                    "rack drop hook preserves every module");
        } finally {
            player.inventory.setInventorySlotContents(9, prior);
            player.setPosition(x, y, z);
        }
    }

    private static void checkSneakInteraction(WorldServer world, EntityPlayerMP player) {
        double x = player.posX, y = player.posY, z = player.posZ;
        boolean sneaking = player.isSneaking();
        ItemStack held = player.getHeldItemMainhand();
        try {
            player.setPosition(RACK.getX() + .5, RACK.getY(), RACK.getZ() + .5);
            player.setSneaking(true);
            for (ItemStack item : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STICK)}) {
                player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, item);
                net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event =
                        new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,
                                net.minecraft.util.EnumHand.MAIN_HAND, RACK, net.minecraft.util.EnumFacing.UP,
                                new net.minecraft.util.math.Vec3d(RACK.getX() + .5, RACK.getY() + 1, RACK.getZ() + .5));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                require(event.isCanceled() && event.getCancellationResult() == net.minecraft.util.EnumActionResult.SUCCESS
                        && player.openContainer instanceof ContainerCpuRack && world.getTileEntity(RACK) == rack,
                        "rack opens via Forge sneak click without pickup (" + (item.isEmpty() ? "empty hand" : "held item") + ")");
                player.closeScreen();
            }
        } finally {
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, held);
            player.setSneaking(sneaking);
            player.setPosition(x, y, z);
        }
    }

    public static void tick(WorldServer world, EntityPlayerMP player, int ticks) throws Exception {
        if (ticks == 100) {
            cpu = (CraftingCPUCluster) rack.getCluster();
            require(cpu != null && cpu.isActive(), "formed powered mixed CPU");
            require(cpu.getAvailableStorage() == (1L << 35) + 1024 + 65536, "native cluster counts long memory");
            require(cpu.getCoProcessors() == 46657 + 9 + 81 + 1, "native cluster counts rack and stock coprocs");
            IGrid grid = rack.getProxy().getGrid();
            ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
            require(crafting.getCpus().contains(cpu), "CPU visible to native network scheduler");
            IAEItemStack output = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)
                    .createStack(new ItemStack(Items.DIAMOND, 8));
            plan = crafting.beginCraftingJob(world, grid, new MachineSource(provider), output, null);
        }
        if (plan != null && plan.isDone() && job == null) {
            ICraftingJob planned = plan.get();
            require(!planned.isSimulation(), "AE2 plans a genuine job");
            job = rack.getProxy().getGrid().<ICraftingGrid>getCache(ICraftingGrid.class)
                    .submitJob(planned, null, cpu, true, new MachineSource(provider));
            require(job != null && cpu.isBusy(), "genuine job starts on the expert CPU");
            double x = player.posX, y = player.posY, z = player.posZ;
            try {
                player.setPosition(RACK.getX() + .5, RACK.getY(), RACK.getZ() + .5);
                ContainerCpuRack gui = new ContainerCpuRack(player, rack);
                require(gui.transferStackInSlot(player, 0).isEmpty(), "busy CPU rejects shift extraction on server");
                gui.slotClick(0, 0, ClickType.PICKUP, player);
                require(gui.inventorySlots.get(0).getStack().getCount() == 64, "busy CPU rejects normal extraction on server");
            } finally {
                player.setPosition(x, y, z);
            }
        }
        if (ticks == 360) {
            require(job != null, "job planner completed within the test window");
            net.minecraft.tileentity.TileEntityChest chest =
                    (net.minecraft.tileentity.TileEntityChest) world.getTileEntity(PROCESSING.north());
            require(chest.getStackInSlot(0).getCount() == 8 && chest.getStackInSlot(0).getItem() == Items.REDSTONE,
                    "all eight processing operations delivered by native scheduler");
            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IAEItemStack result = channel.createStack(new ItemStack(Items.DIAMOND, 8));
            MachineSource source = new MachineSource(provider);
            IAEItemStack leftover = cpu.injectItems(result, Actionable.MODULATE, source);
            require(!cpu.isBusy(), "native CPU acknowledges all returned results");
            require(leftover != null && leftover.getStackSize() == 8, "standalone output is offered to storage");
            require(rack.getProxy().getStorage().getInventory(channel).injectItems(leftover, Actionable.MODULATE, source) == null,
                    "finished output returns to ME storage");
        }
        if (ticks == 420) {
            require(!cpu.isBusy() && !job.isCanceled(), "native standalone job drains its operation queue");
            double x = player.posX, y = player.posY, z = player.posZ;
            try {
                player.setPosition(RACK.getX() + .5, RACK.getY(), RACK.getZ() + .5);
                ContainerCpuRack gui = new ContainerCpuRack(player, rack);
                require(!gui.transferStackInSlot(player, 3).isEmpty(), "idle rack can be reconfigured after job");
                CraftingCPUCluster changed = (CraftingCPUCluster) rack.getCluster();
                require(changed != null && changed.getCoProcessors() == 46656 + 9 + 81 + 1,
                        "CPU rebuild applies new processor count");
            } finally {
                player.setPosition(x, y, z);
            }
            exerciseRackLimits(world, player);
            checkSneakInteraction(world, player);
            System.out.println("[AE2ExpertCPU] DONE passed=" + passed);
        }
    }
}

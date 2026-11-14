package com.e2e.ae2expert;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.util.AEPartLocation;
import com.e2e.ae2expert.wireless.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public final class RuntimeIntegrationChecksWireless {
    private static final BlockPos HUB = new BlockPos(8, 100, 24);
    private static final BlockPos RELAY = HUB.east(12);
    private static WirelessTile hub, relay;
    private static EntityPlayerMP player;
    private static ItemStack linker;
    private static int passed;

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
        passed++;
        System.out.println("[AE2ExpertRadio] PASS " + reason);
    }

    private static WirelessTile place(WorldServer world, BlockPos pos, Block block) {
        world.getChunk(pos);
        world.setBlockState(pos, block.getDefaultState());
        block.onBlockPlacedBy(world, pos, world.getBlockState(pos), player, new ItemStack(block));
        return (WirelessTile) world.getTileEntity(pos);
    }

    static void setup(WorldServer world, EntityPlayerMP owner) {
        player = owner;
        hub = place(world, HUB, WirelessRegistry.HUB);
        relay = place(world, RELAY, WirelessRegistry.BLOCKS.get(4));
        Block energy = AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get();
        world.setBlockState(HUB.up(), energy.getDefaultState());
        linker = new ItemStack(WirelessRegistry.LINKER);
        check(hub.getChannelCapacity() == 256 && relay.getChannelCapacity() == 256, "high-tier node capacity");
    }

    private static void use(WorldServer world, BlockPos pos, boolean sneaking) {
        boolean prior = player.isSneaking();
        ItemStack priorItem = player.getHeldItemMainhand();
        try {
            player.setHeldItem(EnumHand.MAIN_HAND, linker);
            player.setSneaking(sneaking);
            linker.getItem().onItemUse(player, world, pos, EnumHand.MAIN_HAND, EnumFacing.UP, .5f, 1, .5f);
        } finally {
            player.setSneaking(prior);
            player.setHeldItem(EnumHand.MAIN_HAND, priorItem);
        }
    }

    static void tick(WorldServer world, int ticks) {
        if (ticks == 80) {
            use(world, HUB, false);
            use(world, RELAY, false);
            use(world, RELAY, false);
            check(hub.connectionCount() == 1, "second bind is idempotent");
        }
        if (ticks == 120) {
            IGridNode a = hub.getGridNode(AEPartLocation.INTERNAL), b = relay.getGridNode(AEPartLocation.INTERNAL);
            check(a != null && b != null && a.getGrid() == b.getGrid(), "radio joins actual AE2 grids");
            check(hub.snapshot().get(0).online && relay.getProxy().isPowered(), "remote node receives network power");
            WirelessTile saved = new WirelessTile();
            saved.readFromNBT(hub.writeToNBT(new NBTTagCompound()));
            check(saved.connectionCount() == 1 && hub.identity().equals(saved.identity()), "hub identity and pairing survive serialization");
            use(world, RELAY, true);
            check(hub.connectionCount() == 0, "unlink clears hub roster");
        }
        if (ticks == 160) {
            check(hub.getActionableNode().getGrid() != relay.getActionableNode().getGrid(), "unlink splits previously joined networks");
            use(world, RELAY, false);
        }
        if (ticks == 200) {
            check(hub.connectionCount() == 1 && hub.snapshot().get(0).online, "rebind reconnects");
            world.setBlockToAir(RELAY);
        }
        if (ticks == 240) {
            check(hub.connectionCount() == 0, "broken relay releases its list entry");
            checkRosterLimit(world);
            player.connection.setPlayerLocation(HUB.getX() + .5, HUB.getY() + 2, HUB.getZ() + .5, 0, 0);
            player.openGui(AE2ExpertAddon.instance, 3, world, HUB.getX(), HUB.getY(), HUB.getZ());
            System.out.println("[AE2ExpertRadio] DONE passed=" + passed);
        }
    }
    private static void checkRosterLimit(WorldServer world) {
        NBTTagCompound original = hub.writeToNBT(new NBTTagCompound());
        NBTTagCompound full = original.copy();
        net.minecraft.nbt.NBTTagList entries = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < 129; i++) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("pos", new BlockPos(10000 + i, 100, 10000).toLong());
            entry.setUniqueId("id", java.util.UUID.randomUUID());
            entry.setString("name", "Relay " + i);
            entry.setInteger("tier", 32);
            entries.appendTag(entry);
        }
        full.setTag("radioLinks", entries);
        // Seed this test fixture without reloading the live AE2 grid proxy.
        java.util.Map<net.minecraft.util.math.BlockPos, WirelessTile.Link> live = roster(hub);
        WirelessTile saved = new WirelessTile();
        saved.readFromNBT(full);
        live.putAll(roster(saved));
        try {
            check(hub.connectionCount() == 128, "restore caps roster at 128");
            relay = place(world, RELAY, WirelessRegistry.BLOCKS.get(1));
            use(world, HUB, false);
            use(world, RELAY, false);
            check(hub.connectionCount() == 128 && !relay.writeToNBT(new NBTTagCompound()).hasUniqueId("hubId"),
                    "linker rejects relay 129 without modifying relay");
            check(hub.snapshot().stream().noneMatch(entry -> entry.online), "unloaded endpoints remain offline");
        } finally {
            live.clear();
        }
        use(world, RELAY, false);
        check(hub.connectionCount() == 1, "new relay can bind after a slot becomes available");
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<BlockPos, WirelessTile.Link> roster(WirelessTile tile) {
        try {
            java.lang.reflect.Field field = WirelessTile.class.getDeclaredField("links");
            field.setAccessible(true);
            return (java.util.Map<BlockPos, WirelessTile.Link>) field.get(tile);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    static void verifyClient() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
        check(minecraft.player.openContainer instanceof WirelessContainer, "hub GUI opens on client");
        WirelessContainer screen = (WirelessContainer) minecraft.player.openContainer;
        check(screen.entries.size() == 1 && screen.entries.get(0).channels == 32,
                "hub snapshot reaches the client with the correct tier");
        net.minecraft.util.ScreenShotHelper.saveScreenshot(minecraft.gameDir, "wireless-check.png",
                minecraft.displayWidth, minecraft.displayHeight, minecraft.getFramebuffer());
        minecraft.player.closeScreen();
        System.out.println("[AE2ExpertRadio] CLIENT_DONE");
    }
}

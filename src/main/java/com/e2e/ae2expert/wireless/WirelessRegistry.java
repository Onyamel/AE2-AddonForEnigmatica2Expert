package com.e2e.ae2expert.wireless;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class WirelessRegistry {
    public static final WirelessBlock HUB = new WirelessBlock("wireless_hub", 256, true);
    public static final List<WirelessBlock> BLOCKS = Arrays.asList(HUB,
            new WirelessBlock("wireless_relay_32", 32, false), new WirelessBlock("wireless_relay_64", 64, false),
            new WirelessBlock("wireless_relay_128", 128, false), new WirelessBlock("wireless_relay_256", 256, false));
    public static final Item LINKER = new WirelessLinker();
    private WirelessRegistry() { }

    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : BLOCKS) event.getRegistry().register(block);
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS) event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        event.getRegistry().register(LINKER);
    }

    public static void registerTiles() {
        GameRegistry.registerTileEntity(WirelessTile.class, new ResourceLocation("ae2expert", "wireless_node"));
        WirelessPackets.register();
    }
}

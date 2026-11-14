package com.e2e.ae2expert.machine;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class MachineRegistry {
    public static final Block FLUIX_CHAMBER = new BlockMachine("fluix_chamber", false);
    public static final Block RF_EU_CONVERTER = new BlockMachine("rf_eu_converter", true);
    public static final List<Block> BLOCKS = Arrays.asList(FLUIX_CHAMBER, RF_EU_CONVERTER);
    private MachineRegistry() { }
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : BLOCKS) event.getRegistry().register(block);
    }
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS) event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }
    public static void registerTiles() {
        GameRegistry.registerTileEntity(TileFluixChamber.class, new ResourceLocation("ae2expert", "fluix_chamber"));
        GameRegistry.registerTileEntity(TileRfEuConverter.class, new ResourceLocation("ae2expert", "rf_eu_converter"));
    }
}

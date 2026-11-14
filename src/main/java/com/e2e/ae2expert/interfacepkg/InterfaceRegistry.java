package com.e2e.ae2expert.interfacepkg;

import java.util.Arrays;
import java.util.List;
import appeng.core.features.ActivityState;
import appeng.core.features.BlockStackSrc;
import appeng.tile.AEBaseTile;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class InterfaceRegistry {
    public static final List<BlockExpertInterface> BLOCKS = Arrays.asList(
            new BlockExpertInterface(18, TileExpertInterface.S18.class),
            new BlockExpertInterface(27, TileExpertInterface.S27.class),
            new BlockExpertInterface(36, TileExpertInterface.S36.class),
            new BlockExpertInterface(48, TileExpertInterface.S48.class),
            new BlockExpertInterface(64, TileExpertInterface.S64.class));
    private InterfaceRegistry() { }
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : BLOCKS) event.getRegistry().register(block);
    }
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS) event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }
    public static void registerTiles() {
        for (BlockExpertInterface block : BLOCKS) {
            GameRegistry.registerTileEntity(block.getTileEntityClass(), block.getRegistryName());
            AEBaseTile.registerTileItem(block.getTileEntityClass(), new BlockStackSrc(block, 0, ActivityState.Enabled));
        }
    }
}

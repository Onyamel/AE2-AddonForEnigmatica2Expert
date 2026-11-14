package com.e2e.ae2expert.assembler;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import appeng.core.features.ActivityState;
import appeng.core.features.BlockStackSrc;
import appeng.tile.AEBaseTile;

import com.e2e.ae2expert.ModItems;

/**
 * Registration of the addon's molecular assemblers.
 */
public final class AssemblerRegistry {

    public static final Map<AssemblerTier, BlockExpertAssembler> BLOCKS = new EnumMap<>(AssemblerTier.class);

    private static boolean created;

    private AssemblerRegistry() {
    }

    public static void create() {
        if (created) {
            return;
        }
        created = true;
        for (AssemblerTier tier : AssemblerTier.values()) {
            BLOCKS.put(tier, new BlockExpertAssembler(tier));
        }
    }

    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        create();
        for (BlockExpertAssembler block : BLOCKS.values()) {
            event.getRegistry().register(block);
        }
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        create();
        for (BlockExpertAssembler block : BLOCKS.values()) {
            ItemBlock item = new ItemBlock(block);
            item.setRegistryName(block.getRegistryName());
            item.setCreativeTab(ModItems.TAB);
            event.getRegistry().register(item);
        }
    }

    public static void registerTileEntities() {
        create();
        for (BlockExpertAssembler block : BLOCKS.values()) {
            Class<? extends AEBaseTile> tileClass = block.getTileClass();
            GameRegistry.registerTileEntity(tileClass, "ae2expert:" + block.getRegistryName().getPath());
            AEBaseTile.registerTileItem(tileClass, new BlockStackSrc(block, 0, ActivityState.Enabled));
        }
    }
}

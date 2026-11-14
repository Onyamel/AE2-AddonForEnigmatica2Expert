package com.e2e.ae2expert.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.e2e.ae2expert.storage.CellTier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class CpuRegistry {
    public static final List<BlockExpertCpu> BLOCKS;
    public static final BlockExpertCpu RACK;
    static {
        List<BlockExpertCpu> blocks = new ArrayList<>();
        for (CellTier tier : CellTier.values()) {
            if (tier.kilobytes > 64) {
                blocks.add(new BlockExpertCpu("crafting_storage_" + tier.kilobytes + "k", tier.getBytes(), 0, false));
            }
        }
        for (int tier : new int[] { 9, 81, 729 }) {
            blocks.add(new BlockExpertCpu("crafting_coprocessor_" + tier, 0, tier, false));
        }
        RACK = new BlockExpertCpu("crafting_cpu_rack", 0, 0, true);
        blocks.add(RACK);
        BLOCKS = Collections.unmodifiableList(blocks);
    }

    private CpuRegistry() { }

    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (BlockExpertCpu block : BLOCKS) event.getRegistry().register(block);
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (BlockExpertCpu block : BLOCKS) {
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }
    }

    public static void registerTiles() {
        GameRegistry.registerTileEntity(TileExpertCpu.class, new ResourceLocation("ae2expert", "crafting_cpu_tier"));
        GameRegistry.registerTileEntity(TileCpuRack.class, RACK.getRegistryName());
    }
}

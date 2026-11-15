package com.e2e.ae2expert.extended;

import com.e2e.ae2expert.ModItems;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class TableRegistry {
    public static final ItemTablePattern PATTERN = new ItemTablePattern();
    public static final List<BlockTableAssembler> ASSEMBLERS = Arrays.asList(
            new BlockTableAssembler("basic", 3), new BlockTableAssembler("advanced", 5),
            new BlockTableAssembler("elite", 7), new BlockTableAssembler("ultimate", 9));

    public static final List<BlockTableTerminal> TERMINALS = Arrays.asList(
            new BlockTableTerminal("basic", 3), new BlockTableTerminal("advanced", 5),
            new BlockTableTerminal("elite", 7), new BlockTableTerminal("ultimate", 9));

    private TableRegistry() { }

    public static boolean enabled() { return Loader.isModLoaded("extendedcrafting"); }

    public static void registerTiles() {
        if (enabled()) GameRegistry.registerTileEntity(TileTableAssembler.class,
                new ResourceLocation("ae2expert", "table_assembler"));
    }

    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) return;
        ASSEMBLERS.forEach(event.getRegistry()::register);
        TERMINALS.forEach(event.getRegistry()::register);
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) return;
        PATTERN.setCreativeTab(ModItems.TAB);
        event.getRegistry().register(PATTERN);
        ASSEMBLERS.forEach(block -> registerItem(event, block));
        TERMINALS.forEach(block -> registerItem(event, block));
    }

    private static void registerItem(RegistryEvent.Register<Item> event, Block block) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        item.setCreativeTab(ModItems.TAB);
        event.getRegistry().register(item);
    }
}

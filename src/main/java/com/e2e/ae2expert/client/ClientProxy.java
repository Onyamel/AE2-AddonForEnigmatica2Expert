package com.e2e.ae2expert.client;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.e2e.ae2expert.CommonProxy;
import com.e2e.ae2expert.ModItems;
import com.e2e.ae2expert.assembler.AssemblerRegistry;
import com.e2e.ae2expert.assembler.BlockExpertAssembler;
import com.e2e.ae2expert.bus.BusModels;

/**
 * Client side proxy: item and part model registration.
 */
public class ClientProxy extends CommonProxy {

    @Override public Object createGui(int id, net.minecraft.entity.player.EntityPlayer player, net.minecraft.tileentity.TileEntity tile) {
        if (id == 0 && (tile instanceof com.e2e.ae2expert.machine.TileFluixChamber || tile instanceof com.e2e.ae2expert.machine.TileRfEuConverter))
            return new GuiMachines(new com.e2e.ae2expert.machine.ContainerMachine(player, tile));
        if (id == 1 && tile instanceof com.e2e.ae2expert.interfacepkg.TileExpertInterface)
            return new GuiExpertInterface(new com.e2e.ae2expert.interfacepkg.ContainerExpertInterface(player,
                    (com.e2e.ae2expert.interfacepkg.TileExpertInterface) tile));
        return null;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // Part models must be known before the model bakery runs.
        BusModels.register();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        java.util.List<Block> blocks=new java.util.ArrayList<>();
        blocks.addAll(AssemblerRegistry.BLOCKS.values());
        blocks.addAll(com.e2e.ae2expert.interfacepkg.InterfaceRegistry.BLOCKS);
        blocks.addAll(com.e2e.ae2expert.machine.MachineRegistry.BLOCKS);
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        for(Block block:blocks) {
            final String name=block.getRegistryName().getPath();
            mc.getBlockColors().registerBlockColorHandler((state,world,pos,tint)->VisualPalette.color(name,tint),block);
            mc.getItemColors().registerItemColorHandler((stack,tint)->VisualPalette.color(name,tint),Item.getItemFromBlock(block));
        }
        for(com.e2e.ae2expert.item.ItemExpertCable item:ModItems.cables.values())
            mc.getItemColors().registerItemColorHandler((stack,tint)->VisualPalette.cable(item.getChannelCapacity()),item);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    /** Registers a simple inventory model for every item and block item of the addon. */
    public static void registerItemModels(ModelRegistryEvent event) {
        for (Item item : ModItems.all()) {
            if (item.getRegistryName() == null) {
                continue;
            }
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation(item.getRegistryName(), "inventory"));
        }

        for (BlockExpertAssembler block : AssemblerRegistry.BLOCKS.values()) {
            registerBlockItemModel(block);
        }
        for (Block block : com.e2e.ae2expert.machine.MachineRegistry.BLOCKS) {
            registerBlockItemModel(block);
        }
        for (Block block : com.e2e.ae2expert.interfacepkg.InterfaceRegistry.BLOCKS) {
            registerBlockItemModel(block);
        }
    }

    private static void registerBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        if (item == null || block.getRegistryName() == null) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new net.minecraft.client.renderer.block.model.ModelResourceLocation(block.getRegistryName(), "inventory"));
    }
}

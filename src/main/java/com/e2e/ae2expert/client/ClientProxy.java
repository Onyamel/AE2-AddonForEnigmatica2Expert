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
import com.e2e.ae2expert.extended.TableRegistry;
import com.e2e.ae2expert.assembler.BlockExpertAssembler;
import com.e2e.ae2expert.bus.BusModels;

/**
 * Client side proxy: item and part model registration.
 */
public class ClientProxy extends CommonProxy {
    @Override public Object createTableGui(com.e2e.ae2expert.extended.ContainerTableTerminal container) {
        return new GuiTableTerminal(container);
    }

    @Override public Object createGui(int id, net.minecraft.entity.player.EntityPlayer player, net.minecraft.tileentity.TileEntity tile) {
        if (id == 0 && (tile instanceof com.e2e.ae2expert.machine.TileFluixChamber || tile instanceof com.e2e.ae2expert.machine.TileRfEuConverter))
            return new GuiMachines(new com.e2e.ae2expert.machine.ContainerMachine(player, tile));
        if (id == 1 && tile instanceof com.e2e.ae2expert.interfacepkg.TileExpertInterface)
            return new GuiExpertInterface(new com.e2e.ae2expert.interfacepkg.ContainerExpertInterface(player,
                    (com.e2e.ae2expert.interfacepkg.TileExpertInterface) tile));
        if (id == 2 && tile instanceof com.e2e.ae2expert.crafting.TileCpuRack)
            return new GuiCpuRack(new com.e2e.ae2expert.crafting.ContainerCpuRack(player, (com.e2e.ae2expert.crafting.TileCpuRack) tile));
        if (id == 3 && tile instanceof com.e2e.ae2expert.wireless.WirelessTile)
            return new GuiWirelessHub(new com.e2e.ae2expert.wireless.WirelessContainer(player,
                    (com.e2e.ae2expert.wireless.WirelessTile) tile));
        return null;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // Part models must be known before the model bakery runs.
        BusModels.register();
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                com.e2e.ae2expert.encounter.Visitor.class, VisitorRenderer::new);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new WirelessOverlay());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new PatternQuantityGui.InputHandler());
        java.util.List<Block> blocks=new java.util.ArrayList<>();
        blocks.addAll(com.e2e.ae2expert.interfacepkg.InterfaceRegistry.BLOCKS);
        blocks.addAll(com.e2e.ae2expert.machine.MachineRegistry.BLOCKS);
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        for(Block block:blocks) {
            final String name=block.getRegistryName().getPath();
            mc.getBlockColors().registerBlockColorHandler((state,world,pos,tint)->VisualPalette.color(name,tint),block);
            mc.getItemColors().registerItemColorHandler((stack,tint)->VisualPalette.color(name,tint),Item.getItemFromBlock(block));
        }
        for(com.e2e.ae2expert.item.ItemExpertCable item:ModItems.cables.values())
            mc.getItemColors().registerItemColorHandler((stack,tint)->tint == 0 ? item.paintColor(stack.getMetadata(), VisualPalette.cable(item.getChannelCapacity())) : 0xFFFFFF,item);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override public void receiveWirelessList(com.e2e.ae2expert.wireless.WirelessPackets.ListMessage message) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.player != null && mc.player.openContainer instanceof com.e2e.ae2expert.wireless.WirelessContainer
                    && mc.player.openContainer.windowId == message.window) {
                com.e2e.ae2expert.wireless.WirelessContainer hub =
                        (com.e2e.ae2expert.wireless.WirelessContainer) mc.player.openContainer;
                hub.entries = message.links;
                WirelessOverlay.reconcile(hub.position(), message.links);
            }
        });
    }

    /** Registers a simple inventory model for every item and block item of the addon. */
    public static void registerItemModels(ModelRegistryEvent event) {
        if (TableRegistry.enabled()) {
            TableRegistry.ASSEMBLERS.forEach(ClientProxy::registerBlockItemModel);
            TableRegistry.TERMINALS.forEach(ClientProxy::registerBlockItemModel);
            Item pattern = TableRegistry.PATTERN;
            ModelLoader.setCustomModelResourceLocation(pattern, 0,
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation(pattern.getRegistryName(), "inventory"));
        }
        for (Item item : ModItems.all()) {
            if (item.getRegistryName() == null) {
                continue;
            }
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation(item.getRegistryName(), "inventory"));
        }

        for (Item item : ModItems.cables.values()) {
            net.minecraft.client.renderer.block.model.ModelResourceLocation model =
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation(item.getRegistryName(), "inventory");
            for (int meta = 1; meta <= 16; meta++) ModelLoader.setCustomModelResourceLocation(item, meta, model);
        }
        for (Block block : com.e2e.ae2expert.wireless.WirelessRegistry.BLOCKS) registerBlockItemModel(block);
        Item linker = com.e2e.ae2expert.wireless.WirelessRegistry.LINKER;
        ModelLoader.setCustomModelResourceLocation(linker, 0,
                new net.minecraft.client.renderer.block.model.ModelResourceLocation(linker.getRegistryName(), "inventory"));
        for (Block block : com.e2e.ae2expert.crafting.CpuRegistry.BLOCKS) registerBlockItemModel(block);
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

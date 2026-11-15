package com.e2e.ae2expert;

import appeng.api.AEApi;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Server side proxy. Holds everything that is safe on a dedicated server.
 */
public class CommonProxy {
    public Object createTableGui(com.e2e.ae2expert.extended.ContainerTableTerminal container) { return null; }
    public void receiveWirelessList(com.e2e.ae2expert.wireless.WirelessPackets.ListMessage message) { }


    public Object createGui(int id, net.minecraft.entity.player.EntityPlayer player, net.minecraft.tileentity.TileEntity tile) {
        return null;
    }

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        registerCellHandlers();
        for (net.minecraft.item.Item item : ModItems.all()) {
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
            if (item instanceof com.e2e.ae2expert.item.ExpertStorageCell) {
                appeng.api.config.Upgrades.FUZZY.registerItem(stack, 1);
                appeng.api.config.Upgrades.INVERTER.registerItem(stack, 1);
            }
            if (item instanceof com.e2e.ae2expert.item.ItemExpertBus) {
                appeng.api.config.Upgrades.SPEED.registerItem(stack, 4);
                appeng.api.config.Upgrades.CAPACITY.registerItem(stack, 2);
                appeng.api.config.Upgrades.REDSTONE.registerItem(stack, 1);
                appeng.api.config.Upgrades.FUZZY.registerItem(stack, 1);
                appeng.api.config.Upgrades.CRAFTING.registerItem(stack, 1);
            }
        }
        for (net.minecraft.block.Block block : com.e2e.ae2expert.assembler.AssemblerRegistry.BLOCKS.values())
            appeng.api.config.Upgrades.SPEED.registerItem(new net.minecraft.item.ItemStack(block), 5);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    /** Свои обработчики long регистрируются после штатного обработчика AE2. */
    protected void registerCellHandlers() {
        for (net.minecraft.item.Item item : ModItems.all()) {
            if (item instanceof appeng.api.storage.ICellHandler)
                AEApi.instance().registries().cell().addCellHandler((appeng.api.storage.ICellHandler) item);
        }
    }
}

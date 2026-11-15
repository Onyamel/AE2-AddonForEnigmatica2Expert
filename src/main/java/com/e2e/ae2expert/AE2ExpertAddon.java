package com.e2e.ae2expert;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.e2e.ae2expert.assembler.AssemblerRegistry;
import com.e2e.ae2expert.extended.TableRegistry;
import com.e2e.ae2expert.crafting.CpuRegistry;
import com.e2e.ae2expert.machine.MachineRegistry;
import com.e2e.ae2expert.interfacepkg.InterfaceRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * AE2 Expert Addon - an Applied Energistics 2 companion mod tuned for Enigmatica 2: Expert.
 */
@Mod(modid = AE2ExpertAddon.MODID,
        name = AE2ExpertAddon.NAME,
        version = AE2ExpertAddon.VERSION,
        dependencies = "required-after:appliedenergistics2;required-after:ic2;required-after:redstoneflux;after:extracells;after:mekanism;after:extendedcrafting",
        acceptedMinecraftVersions = "[1.12,1.13)",
        acceptableRemoteVersions = "[3.0.0]")
public class AE2ExpertAddon {

    public static final String MODID = "ae2expert";
    public static final String NAME = "AE2 Expert Addon";
    public static final String VERSION = "3.0.0";

    @Mod.Instance(MODID)
    public static AE2ExpertAddon instance;

    @SidedProxy(clientSide = "com.e2e.ae2expert.client.ClientProxy",
            serverSide = "com.e2e.ae2expert.CommonProxy")
    public static CommonProxy proxy;

    private com.e2e.ae2expert.encounter.VisitController worldVisit;

    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        worldVisit = com.e2e.ae2expert.encounter.VisitController.start(event);
    }

    @Mod.EventHandler
    public void serverStopped(net.minecraftforge.fml.common.event.FMLServerStoppedEvent event) {
        com.e2e.ae2expert.encounter.VisitController.stop(worldVisit);
        worldVisit = null;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        AssemblerRegistry.registerTileEntities();
        TableRegistry.registerTiles();
        CpuRegistry.registerTiles();
        PatternQuantity.register();
        com.e2e.ae2expert.encounter.VisitController.register();
        com.e2e.ae2expert.extended.TableRecipeTransfer.register();
        net.minecraftforge.fml.common.event.FMLInterModComms.sendMessage(
                "carryon", "blacklistBlock", CpuRegistry.RACK.getRegistryName().toString());
        com.e2e.ae2expert.wireless.WirelessRegistry.registerTiles();
        MachineRegistry.registerTiles();
        InterfaceRegistry.registerTiles();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    /** Registry hooks of the addon. */
    @Mod.EventBusSubscriber(modid = MODID)
    public static final class RegistryHandler {

        @SubscribeEvent
        public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
            AssemblerRegistry.registerBlocks(event);
            TableRegistry.registerBlocks(event);
            CpuRegistry.registerBlocks(event);
            com.e2e.ae2expert.wireless.WirelessRegistry.registerBlocks(event);
            MachineRegistry.registerBlocks(event);
            InterfaceRegistry.registerBlocks(event);
        }

        @SubscribeEvent
        public static void onRegisterItems(RegistryEvent.Register<Item> event) {
            ModItems.createItems();
            ModItems.registerAll(event.getRegistry());
            AssemblerRegistry.registerItems(event);
            TableRegistry.registerItems(event);
            CpuRegistry.registerItems(event);
            com.e2e.ae2expert.wireless.WirelessRegistry.registerItems(event);
            MachineRegistry.registerItems(event);
            InterfaceRegistry.registerItems(event);
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void onRegisterModels(ModelRegistryEvent event) {
            com.e2e.ae2expert.client.ClientProxy.registerItemModels(event);
        }
    }
}

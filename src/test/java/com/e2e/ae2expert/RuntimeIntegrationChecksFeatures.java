package com.e2e.ae2expert;

import java.util.*;
import appeng.api.AEApi;
import appeng.api.config.*;
import appeng.api.definitions.IParts;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.IGridHost;
import appeng.api.parts.*;
import appeng.api.util.*;
import appeng.container.implementations.ContainerUpgradeable;
import com.e2e.ae2expert.api.INineSlotBus;
import com.e2e.ae2expert.cable.CableTier;
import com.e2e.ae2expert.interfacepkg.InterfaceRegistry;
import com.e2e.ae2expert.integration.jei.ExpertJeiPlugin;
import com.e2e.ae2expert.machine.*;
import com.e2e.ae2expert.storage.CellTier;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.recipe.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.*;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Новые сценарии 1.2.0: живые плотные кабели, фильтр №9, настоящие индексы рецептов HEI. */
public final class RuntimeIntegrationChecksFeatures {
    private static final List<IPart> parts=new ArrayList<>();
    private static final List<IGridHost> blockInterfaces=new ArrayList<>();
    private static final List<net.minecraft.inventory.IInventory> imports=new ArrayList<>(),exports=new ArrayList<>();
    private static int passed;
    private static void require(boolean value,String message) {
        if(!value)throw new AssertionError(message);
        passed++;System.out.println("[AE2ExpertFeatures] PASS "+message);
    }
    private static void place(WorldServer world,BlockPos at,Block block) {
        world.getChunk(at);world.setBlockToAir(at);world.setBlockState(at,block.getDefaultState());
    }
    public static void setup(WorldServer world,EntityPlayerMP player)throws Exception {
        IParts definitions=AEApi.instance().definitions().parts();
        List<ItemStack> cables=new ArrayList<>();
        for(CableTier tier:CableTier.values())cables.add(new ItemStack(ModItems.cables.get(tier)));
        cables.add(definitions.cableDenseSmart().stack(AEColor.TRANSPARENT,1));
        cables.add(definitions.cableDenseCovered().stack(AEColor.TRANSPARENT,1));
        List<ItemStack> attachments=new ArrayList<>();
        for(appeng.api.definitions.IItemDefinition definition:Arrays.asList(definitions.craftingTerminal(),definitions.terminal(),
                definitions.patternTerminal(),definitions.interfaceTerminal(),definitions.iface(),definitions.fluidIface(),
                definitions.storageBus(),definitions.importBus(),definitions.exportBus(),definitions.fluidStorageBus(),
                definitions.fluidImportBus(),definitions.fluidExportBus(),definitions.fluidTerminal()))
            definition.maybeStack(1).ifPresent(attachments::add);
        ModItems.importBuses.values().forEach(item->attachments.add(new ItemStack(item)));
        ModItems.exportBuses.values().forEach(item->attachments.add(new ItemStack(item)));
        for(int group=0;group<cables.size();group++) {
            BlockPos origin=new BlockPos(0,86,-28+group*4);
            for(int x=-3;x<attachments.size()+6;x++)for(int y=0;y<=1;y++)for(int z=0;z<=1;z++)world.setBlockToAir(origin.add(x,y,z));
            place(world,origin.add(-1,0,0),AEApi.instance().definitions().blocks().controller().maybeBlock().get());
            place(world,origin.add(-2,0,0),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
            BlockPos drivePos=origin.add(-1,1,0);
            place(world,drivePos,AEApi.instance().definitions().blocks().drive().maybeBlock().get());
            ItemStack cell=new ItemStack(ModItems.itemCells.get(CellTier.K16));
            appeng.api.storage.IStorageChannel<appeng.api.storage.data.IAEItemStack> channel=AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IItemStorageChannel.class);
            appeng.api.storage.ICellInventoryHandler<appeng.api.storage.data.IAEItemStack> inv=AEApi.instance().registries().cell().getCellInventory(cell,null,channel);
            for(net.minecraft.item.Item item:Arrays.asList(Items.REDSTONE,Items.QUARTZ,Items.GLOWSTONE_DUST))inv.injectItems(channel.createStack(new ItemStack(item,64)),Actionable.MODULATE,null);
            ((appeng.tile.storage.TileDrive)world.getTileEntity(drivePos)).getInternalInventory().insertItem(0,cell,false);
            for(int x=0;x<attachments.size()+6;x++) {
                BlockPos at=origin.add(x,0,0);
                place(world,at,AEApi.instance().definitions().blocks().multiPart().maybeBlock().get());
                IPartHost host=(IPartHost)world.getTileEntity(at);
                ItemStack cable=cables.get(group).copy();
                if(x==0) {
                    require(host.addPart(attachments.get(x).copy(),AEPartLocation.SOUTH,player,EnumHand.MAIN_HAND)!=null,"Терминал до кабеля, группа "+group);
                    require(host.addPart(cable,AEPartLocation.INTERNAL,player,EnumHand.MAIN_HAND)!=null,"Плотный кабель после терминала, группа "+group);
                } else {
                    require(host.addPart(cable,AEPartLocation.INTERNAL,player,EnumHand.MAIN_HAND)!=null,"Плотный кабель, группа "+group+" x="+x);
                    if(x<attachments.size())require(host.addPart(attachments.get(x).copy(),AEPartLocation.SOUTH,player,EnumHand.MAIN_HAND)!=null,"Часть на плотном кабеле, группа "+group+" x="+x);
                }
                if(x<attachments.size()) {
                    IPart part=host.getPart(AEPartLocation.SOUTH);parts.add(part);
                    require(!world.getCollisionBoxes(null,new net.minecraft.util.math.AxisAlignedBB(at)).isEmpty(),"Коллизии плотного кабеля с внутренним соединением безопасны");
                    if(part instanceof INineSlotBus) {
                        IUpgradeableHost bus=(IUpgradeableHost)part;
                        require(bus.getInstalledUpgrades(Upgrades.CAPACITY)==0,"Нет виртуальных или установленных карт ёмкости");
                        ContainerUpgradeable gui=new ContainerUpgradeable(player.inventory,bus);
                        require(gui.isSlotEnabled(1)&&gui.isSlotEnabled(2),"Обе группы дополнительных фильтров открыты");
                        IItemHandlerModifiable config=(IItemHandlerModifiable)bus.getInventoryByName("config");
                        for(int slot=0;slot<8;slot++)config.setStackInSlot(slot,new ItemStack(Items.DIAMOND));
                        int tier=((com.e2e.ae2expert.item.ItemExpertBus)attachments.get(x).getItem()).getTier().ordinal();
                        net.minecraft.item.Item item=new net.minecraft.item.Item[]{Items.REDSTONE,Items.QUARTZ,Items.GLOWSTONE_DUST}[tier];
                        config.setStackInSlot(8,new ItemStack(item));
                        place(world,at.south(),Blocks.DROPPER);
                        net.minecraft.inventory.IInventory chest=(net.minecraft.inventory.IInventory)world.getTileEntity(at.south());
                        if(part instanceof appeng.parts.automation.PartImportBus) {chest.setInventorySlotContents(0,new ItemStack(item,64));imports.add(chest);}
                        else exports.add(chest);
                    } else if(part instanceof appeng.parts.automation.PartImportBus||part instanceof appeng.parts.automation.PartExportBus) {
                        ContainerUpgradeable gui=new ContainerUpgradeable(player.inventory,(IUpgradeableHost)part);
                        require(!gui.isSlotEnabled(1)&&!gui.isSlotEnabled(2),"Ванильные шины сохраняют необходимость карт ёмкости");
                    }
                } else {
                    Block block=x==attachments.size()+5?AEApi.instance().definitions().blocks().iface().maybeBlock().get():InterfaceRegistry.BLOCKS.get(x-attachments.size());
                    place(world,at.up(),block);blockInterfaces.add((IGridHost)world.getTileEntity(at.up()));
                }
            }
        }
        System.out.println("[AE2ExpertFeatures] SETUP parts="+parts.size()+" interfaces="+blockInterfaces.size());
    }
    public static void verify(EntityPlayerMP player) {
        for(IPart part:parts)require(part.getGridNode()!=null&&part.getGridNode().isActive(),"Часть получает канал: "+part.getClass().getSimpleName());
        for(IGridHost block:blockInterfaces)require(block.getGridNode(AEPartLocation.DOWN).isActive(),"Блочный интерфейс получает канал от плотного кабеля");
        for(net.minecraft.inventory.IInventory chest:imports)require(chest.getStackInSlot(0).getCount()<64,"Импорт по фильтру №9 без карт ёмкости");
        for(net.minecraft.inventory.IInventory chest:exports)require(!chest.getStackInSlot(0).isEmpty(),"Экспорт по фильтру №9 без карт ёмкости");
        System.out.println("[AE2ExpertFeatures] SERVER_DONE passed="+passed+" failed=0");
    }
    @SuppressWarnings({"rawtypes","unchecked"}) public static void jei() {
        try {
            require(ExpertJeiPlugin.runtime!=null,"JEI/HEI обнаружил плагин аддона");
            IRecipeRegistry registry=ExpertJeiPlugin.runtime.getRecipeRegistry();
            IRecipeCategory category=registry.getRecipeCategory(FluixChamberRecipe.UID);
            require(category!=null,"Категория камеры зарегистрирована");
            require(registry.getRecipeWrappers(category).size()==1,"Зарегистрирован один рецепт обработки");
            require(!registry.getRecipeCatalysts(category).isEmpty(),"Камера видна как станок-катализатор");
            List<IRecipeCategory> output=registry.getRecipeCategories(registry.createFocus(IFocus.Mode.OUTPUT,FluixChamberRecipe.INSTANCE.output()));
            require(output.stream().anyMatch(c->c.getUid().equals(FluixChamberRecipe.UID)),"Категория камеры есть среди всех способов получения изменчивого кристалла (R)");
            require(output.size()>1,"Сохранены другие способы получения, включая исходные категории AE2");
            for(ItemStack input:FluixChamberRecipe.INSTANCE.inputs()) {
                List<IRecipeCategory> uses=registry.getRecipeCategories(registry.createFocus(IFocus.Mode.INPUT,input));
                require(uses.stream().anyMatch(c->c.getUid().equals(FluixChamberRecipe.UID)),"Ингредиент находит камеру по U: "+input.getDisplayName());
            }
            List<String> ids=new ArrayList<>();ids.add(FluixChamberRecipe.UID);
            for(IRecipeCategory item:output)if(!ids.contains(item.getUid()))ids.add(item.getUid());
            ExpertJeiPlugin.runtime.getRecipesGui().showCategories(Collections.singletonList(FluixChamberRecipe.UID));
            System.out.println("[AE2ExpertFeatures] JEI_DONE passed="+passed+" failed=0 categories="+ids);
        } catch(Throwable failure) {System.out.println("[AE2ExpertFeatures] JEI_FATAL "+failure);failure.printStackTrace();}
    }
}

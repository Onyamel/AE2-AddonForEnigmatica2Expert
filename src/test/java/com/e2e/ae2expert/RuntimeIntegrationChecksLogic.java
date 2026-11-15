package com.e2e.ae2expert;

import java.lang.reflect.Method;
import java.util.*;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartHost;
import appeng.api.storage.*;
import appeng.api.storage.channels.*;
import appeng.api.storage.data.*;
import appeng.api.util.*;
import appeng.container.ContainerNull;
import appeng.container.implementations.ContainerCellWorkbench;
import appeng.tile.crafting.TileMolecularAssembler;
import appeng.tile.misc.TileCellWorkbench;
import appeng.tile.storage.TileDrive;
import com.e2e.ae2expert.assembler.*;
import com.e2e.ae2expert.interfacepkg.*;
import com.e2e.ae2expert.item.*;
import com.e2e.ae2expert.machine.*;
import com.e2e.ae2expert.storage.CellTier;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.*;
import net.minecraftforge.items.*;

/** Сценарии ошибок логики выполняются на настоящем серверном мире E2E. */
public final class RuntimeIntegrationChecksLogic {
    private static int passed;
    private static final List<Bench> benches = new ArrayList<>();
    private static TileFluixChamber automatedChamber;
    private static TileDrive chamberDrive;
    private static TileExpertInterface processor;
    private static BulkTile bulk;
    private static TileExpertAssembler reloaded;
    private static TileEntityChest reloadChest;
    private static int savedJobs, outputPerCraft;
    private static ICraftingPatternDetails craft;
    private static ItemStack craftOutput;

    private static void require(boolean value, String text) {
        if (!value) throw new AssertionError(text);
        passed++;
        System.out.println("[AE2ExpertLogic] PASS " + text);
    }

    public static void register() {
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(BulkTile.class,
                new ResourceLocation("ae2expert_runtime_checks", "bulk_receiver"));
    }

    private static void place(WorldServer world, BlockPos pos, Block block) {
        world.getChunk(pos);
        world.setBlockToAir(pos);
        world.setBlockState(pos, block.getDefaultState());
    }

    public static void setup(WorldServer world, EntityPlayerMP player) throws Exception {
        for (int x = -44; x <= -16; x++) for (int z = -3; z <= 19; z++)
            for (int y = 82; y <= 83; y++) world.setBlockToAir(new BlockPos(x,y,z));
        partition(world, player);

        BlockPos at = new BlockPos(-40,82,0);
        place(world, at, MachineRegistry.FLUIX_CHAMBER);
        automatedChamber = (TileFluixChamber) world.getTileEntity(at);
        BlockPos cable = at.north();
        place(world,cable,AEApi.instance().definitions().blocks().multiPart().maybeBlock().get());
        IPartHost host=(IPartHost)world.getTileEntity(cable);
        host.addPart(AEApi.instance().definitions().parts().cableGlass().stack(AEColor.TRANSPARENT,1),AEPartLocation.INTERNAL,player,EnumHand.MAIN_HAND);
        host.addPart(AEApi.instance().definitions().parts().importBus().maybeStack(1).get(),AEPartLocation.SOUTH,player,EnumHand.MAIN_HAND);
        place(world,cable.north(),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        place(world,cable.west(),AEApi.instance().definitions().blocks().drive().maybeBlock().get());
        chamberDrive=(TileDrive)world.getTileEntity(cable.west());
        chamberDrive.getInternalInventory().insertItem(0,new ItemStack(ModItems.itemCells.get(CellTier.K16)),false);

        BlockPos process = new BlockPos(-32,82,0);
        place(world,process,InterfaceRegistry.BLOCKS.get(0));
        processor=(TileExpertInterface)world.getTileEntity(process);
        place(world,process.north(),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        place(world,process.east(),Blocks.CHEST);
        bulk=new BulkTile();world.setTileEntity(process.east(),bulk);
        InventoryCrafting inputs=table();
        inputs.setInventorySlotContents(0,new ItemStack(Items.REDSTONE,64));
        inputs.setInventorySlotContents(1,new ItemStack(Items.REDSTONE,64));
        processor.patterns.setStackInSlot(0,pattern(false,inputs,new ItemStack(Items.DIAMOND)));

        InventoryCrafting recipe=table();recipe.setInventorySlotContents(0,new ItemStack(Blocks.PLANKS));
        craftOutput=CraftingManager.findMatchingResult(recipe,world);
        require(!craftOutput.isEmpty(),"Найден настоящий рецепт сборщика в E2E");
        outputPerCraft=craftOutput.getCount();
        ItemStack encoded=pattern(true,recipe,craftOutput);
        craft=((appeng.api.implementations.ICraftingPatternItem)encoded.getItem()).getPatternForItem(encoded,world);
        for (AssemblerTier tier : new AssemblerTier[]{AssemblerTier.X2,AssemblerTier.X32}) {
            BlockPos pos = new BlockPos(tier==AssemblerTier.X2?-40:-34,82,10);
            place(world,pos,AssemblerRegistry.BLOCKS.get(tier));
            place(world,pos.north(),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
            place(world,pos.south(),Blocks.CHEST);
            benches.add(new Bench((TileExpertAssembler)world.getTileEntity(pos),(TileEntityChest)world.getTileEntity(pos.south())));
        }
        BlockPos saved=new BlockPos(-26,82,10);
        place(world,saved,AssemblerRegistry.BLOCKS.get(AssemblerTier.X32));
        place(world,saved.north(),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        place(world,saved.south(),Blocks.CHEST);
        reloaded=(TileExpertAssembler)world.getTileEntity(saved);
        reloadChest=(TileEntityChest)world.getTileEntity(saved.south());
    }

    private static void partition(WorldServer world, EntityPlayerMP player) {
        BlockPos at=new BlockPos(-40,82,18);
        place(world,at,AEApi.instance().definitions().blocks().cellWorkbench().maybeBlock().get());
        TileCellWorkbench workbench=(TileCellWorkbench)world.getTileEntity(at);
        ItemExpertGasCell gas=(ItemExpertGasCell)ModItems.all().stream().filter(item->item instanceof ItemExpertGasCell).findFirst().get();
        List<ExpertStorageCell<?>> cells=Arrays.asList(ModItems.itemCells.get(CellTier.K16),ModItems.fluidCells.get(CellTier.K16),gas,ModItems.infiniteCell);
        for(ExpertStorageCell<?> item:cells) partitionOne(workbench,player,item);
        // Штатный предметный обработчик проходит по исходной ветке AE2.
        ItemStack vanilla=AEApi.instance().definitions().items().cell1k().maybeStack(1).get();
        IItemStorageChannel channel=AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        AEApi.instance().registries().cell().getCellInventory(vanilla,null,channel).injectItems(channel.createStack(new ItemStack(Items.QUARTZ)),Actionable.MODULATE,null);
        ((IItemHandlerModifiable)workbench.getInventoryByName("cell")).setStackInSlot(0,vanilla);
        new ContainerCellWorkbench(player.inventory,workbench).partition();
        require(workbench.getInventoryByName("config").getStackInSlot(0).getItem()==Items.QUARTZ,"Разбиение оригинальной ячейки AE2 сохранено");
    }

    private static <T extends IAEStack<T>> void partitionOne(TileCellWorkbench workbench,EntityPlayerMP player,ExpertStorageCell<T> item) {
        ItemStack cell=new ItemStack(item);
        IStorageChannel<T> channel=item.getChannel();
        Object sample=item instanceof ItemExpertGasCell
                ? new mekanism.api.gas.GasStack(mekanism.api.gas.GasRegistry.getGas("hydrogen"),1000)
                : item instanceof ItemExpertFluidCell ? new FluidStack(FluidRegistry.WATER,1000) : new ItemStack(Items.REDSTONE);
        T key=channel.createStack(sample);
        item.getCellInventory(cell,null,channel).injectItems(key.copy(),Actionable.MODULATE,null);
        ((IItemHandlerModifiable)workbench.getInventoryByName("cell")).setStackInSlot(0,cell);
        new ContainerCellWorkbench(player.inventory,workbench).partition();
        ICellInventoryHandler<T> filtered=item.getCellInventory(cell,null,channel);
        require(!workbench.getInventoryByName("config").getStackInSlot(0).isEmpty()&&filtered.isPreformatted(),"Живой верстак задаёт фильтр: "+item.getRegistryName());
        require(filtered.injectItems(key.copy(),Actionable.SIMULATE,null)==null,"Фильтр принимает выбранный ресурс: "+item.getRegistryName());
        if(item instanceof ItemExpertGasCell) {
            T other=channel.createStack(new mekanism.api.gas.GasStack(mekanism.api.gas.GasRegistry.getGas("oxygen"),1000));
            require(filtered.injectItems(other,Actionable.SIMULATE,null)!=null,"Газовый фильтр действительно отвергает другой газ");
        }
        require(item.getCellInventory(cell,null,channel).getCellInv().getStoredItemCount()==key.getStackSize(),"Разбиение не меняет содержимое: "+item.getRegistryName());
        ((IItemHandlerModifiable)workbench.getInventoryByName("cell")).setStackInSlot(0,ItemStack.EMPTY);
    }

    public static void tick(WorldServer world,int tick) throws Exception {
        if(tick==120) {
            IItemHandler external=automatedChamber.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,EnumFacing.NORTH);
            require(external.insertItem(0,ModMachineItems.ae2Material(1,1),false).isEmpty(),"Камера принимает заряженный кварц через capability");
            require(external.insertItem(2,new ItemStack(Items.REDSTONE),false).isEmpty()&&external.insertItem(1,new ItemStack(Items.QUARTZ),false).isEmpty(),"Камера принимает редстоун и кварц в обратном порядке");
            require(external.extractItem(0,1,true).isEmpty()&&external.extractItem(1,1,false).isEmpty(),"Автоматика не извлекает входы камеры");
            for(int i=0;i<4;i++)automatedChamber.receiveEnergy(EnumFacing.UP,1000,false);
            List<ICraftingPatternDetails> options=new ArrayList<>();
            processor.provideCrafting(new appeng.api.networking.crafting.ICraftingProviderHelper(){
                @Override public void addCraftingOption(appeng.api.networking.crafting.ICraftingMedium m,ICraftingPatternDetails p){options.add(p);}
                @Override public void setEmitable(IAEItemStack item){}
            });
            InventoryCrafting inputs=table();inputs.setInventorySlotContents(0,new ItemStack(Items.REDSTONE,64));inputs.setInventorySlotContents(1,new ItemStack(Items.REDSTONE,64));
            require(options.size()==1&&processor.pushPattern(options.get(0),inputs),"Живой интерфейс принимает обработку 2×64 в один большой слот");
            require(bulk.items.getStackInSlot(0).getCount()==128,"Живой приёмник получил ровно 128 предметов");
            mergeFirst(processor);
            for(int i=0;i<40;i++)if(reloaded.pushPattern(craft,craftInput(),EnumFacing.SOUTH))savedJobs++;
            require(savedJobs>1&&savedJobs<=33,"Очередь сборщика ограничена: принято "+savedJobs);
            List<ItemStack> drops=new ArrayList<>();reloaded.getDrops(world,reloaded.getPos(),drops);
            int planks=drops.stream().filter(stack->stack.getItem()==net.minecraft.item.Item.getItemFromBlock(Blocks.PLANKS)).mapToInt(ItemStack::getCount).sum();
            require(planks==savedJobs,"Разрушение возвращает все принятые ингредиенты, включая очередь");
            NBTTagCompound saved=reloaded.writeToNBT(new NBTTagCompound());
            BlockPos pos=reloaded.getPos();int queue=reloaded.getQueuedCraftCount();
            reloaded.onChunkUnload();world.removeTileEntity(pos);reloaded.invalidate();
            reloaded=(TileExpertAssembler)TileEntity.create(world,saved);world.setTileEntity(pos,reloaded);
            require(reloaded.getQueuedCraftCount()==queue,"Очередь сохранена при сериализации и повторной загрузке тайла");
        }
        if(tick>=120&&tick<160)for(Bench bench:benches)
            for(int i=0;i<40;i++)if(bench.tile.pushPattern(craft,craftInput(),EnumFacing.SOUTH))bench.accepted++;
        if(tick==160) {
            int slow=outputs(benches.get(0).chest),fast=outputs(benches.get(1).chest);
            require(slow>0&&fast>slow*3,"Поток автокрафта за 40 тиков: ×2="+slow+", ×32="+fast);
        }
        if(tick==400) {
            IItemStorageChannel channel=AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            ICellInventoryHandler<IAEItemStack> cell=AEApi.instance().registries().cell().getCellInventory(chamberDrive.getInternalInventory().getStackInSlot(0),null,channel);
            IAEItemStack produced=cell.getAvailableItems(channel.createList()).findPrecise(channel.createStack(ModMachineItems.fluixCrystal()));
            require(produced!=null&&produced.getStackSize()==2&&cell.getCellInv().getStoredItemCount()==2,"Настоящая шина AE2 без фильтра забрала только два кристалла, сырьё переработано");
            require(automatedChamber.getEnergyStored()==0,"Камера с шиной списала ровно 4000 RF");
            for(Bench bench:benches)require(outputs(bench.chest)==bench.accepted*outputPerCraft&&bench.tile.getQueuedCraftCount()==0,"Все задания сборщика выполнены без потерь/дюпа: "+bench.accepted);
            require(outputs(reloadChest)==savedJobs*outputPerCraft&&reloaded.getQueuedCraftCount()==0,"После загрузки выполнены все "+savedJobs+" заданий без потерь/дюпа");
            System.out.println("[AE2ExpertLogic] DONE passed="+passed+" failed=0");
        }
    }

    private static void mergeFirst(TileExpertInterface tile) throws Exception {
        Method fit=TileExpertInterface.class.getDeclaredMethod("canFit",IItemHandler.class,InventoryCrafting.class);fit.setAccessible(true);
        ItemStackHandler receiver=new ItemStackHandler(2);receiver.setStackInSlot(1,new ItemStack(Items.REDSTONE,63));
        InventoryCrafting inputs=table();inputs.setInventorySlotContents(0,new ItemStack(Items.REDSTONE));inputs.setInventorySlotContents(1,new ItemStack(Items.QUARTZ,64));
        require((boolean)fit.invoke(tile,receiver,inputs),"Симуляция дополняет существующий стак до занятия пустого слота");
        require(receiver.getStackInSlot(0).isEmpty()&&receiver.getStackInSlot(1).getCount()==63,"Симуляция не изменила приёмник");
        receiver.setStackInSlot(1,new ItemStack(Items.REDSTONE,64));
        require(!(boolean)fit.invoke(tile,receiver,inputs),"Один пустой слот нельзя обещать двум разным ингредиентам");
    }

    private static int outputs(TileEntityChest chest){int total=0;for(int i=0;i<chest.getSizeInventory();i++)if(ItemHandlerHelper.canItemStacksStack(chest.getStackInSlot(i),craftOutput))total+=chest.getStackInSlot(i).getCount();return total;}
    private static InventoryCrafting table(){return new InventoryCrafting(new ContainerNull(),3,3);}
    private static InventoryCrafting craftInput(){InventoryCrafting t=table();t.setInventorySlotContents(0,new ItemStack(Blocks.PLANKS));return t;}
    private static ItemStack pattern(boolean crafting,InventoryCrafting table,ItemStack output){
        ItemStack encoded=AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tag=new NBTTagCompound();tag.setBoolean("crafting",crafting);
        NBTTagList in=new NBTTagList(),out=new NBTTagList();
        for(int i=0;i<9;i++)in.appendTag(table.getStackInSlot(i).isEmpty()?new NBTTagCompound():table.getStackInSlot(i).writeToNBT(new NBTTagCompound()));
        out.appendTag(output.writeToNBT(new NBTTagCompound()));tag.setTag("in",in);tag.setTag("out",out);encoded.setTagCompound(tag);return encoded;
    }
    private static final class Bench {final TileExpertAssembler tile;final TileEntityChest chest;int accepted;Bench(TileExpertAssembler t,TileEntityChest c){tile=t;chest=c;}}
    public static final class BulkTile extends TileEntity {
        final ItemStackHandler items=new ItemStackHandler(1){
            @Override public int getSlotLimit(int slot){return 2048;}
            @Override protected int getStackLimit(int slot,ItemStack stack){return 2048;}
            @Override protected void onContentsChanged(int slot){markDirty();}
        };
        @Override public boolean hasCapability(Capability<?> cap,EnumFacing side){return cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY||super.hasCapability(cap,side);}
        @Override public <T>T getCapability(Capability<T> cap,EnumFacing side){return cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY?CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(items):super.getCapability(cap,side);}
        @Override public NBTTagCompound writeToNBT(NBTTagCompound tag){super.writeToNBT(tag);tag.setTag("items",items.serializeNBT());return tag;}
        @Override public void readFromNBT(NBTTagCompound tag){super.readFromNBT(tag);items.deserializeNBT(tag.getCompoundTag("items"));}
    }
}

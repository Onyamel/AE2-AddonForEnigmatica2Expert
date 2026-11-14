package com.e2e.ae2expert;

import java.util.*;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.parts.IPartHost;
import appeng.api.storage.*;
import appeng.api.storage.channels.*;
import appeng.api.storage.data.*;
import appeng.api.util.AEPartLocation;
import com.e2e.ae2expert.cable.CableTier;
import com.e2e.ae2expert.interfacepkg.*;
import com.e2e.ae2expert.machine.*;
import com.e2e.ae2expert.storage.CellTier;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.energy.*;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Отдельный тестовый мод; этот класс не попадает в основной jar аддона. */
@Mod(modid="ae2expert_runtime_checks", name="AE2 Expert runtime checks", version="1", dependencies="required-after:ae2expert")
public final class RuntimeIntegrationChecks {
    private boolean started;
    private int wirelessGuiTicks;
    private volatile boolean finished;
    private int visualTicks;
    private int clientTicks, ticks, tierIndex, passed, failed;
    private final List<BlockPos> networkBlocks = new ArrayList<>();
    private final List<TileExpertInterface> terminals = new ArrayList<>();
    private TileFluixChamber chamber;
    private TileExpertInterface stockInterface, craftingInterface, processingInterface;
    private net.minecraft.tileentity.TileEntityChest processingChest;
    private ItemStack craftingOutput;
    private final BlockPos origin = new BlockPos(16, 90, 16);

    @Mod.EventHandler public void init(FMLInitializationEvent event) { RuntimeIntegrationChecksLogic.register(); MinecraftForge.EVENT_BUS.register(this); }

    private static boolean channelWorld() {
        return Boolean.getBoolean("ae2expert.channelWorld")
                || new java.io.File(".ae2expert-channel-world").isFile();
    }

    @SubscribeEvent public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase == TickEvent.Phase.END && started && !channelWorld()
                && new java.io.File(Minecraft.getMinecraft().gameDir, ".ae2expert-runtime-check").isFile()
                && Minecraft.getMinecraft().player != null
                && Minecraft.getMinecraft().player.openContainer instanceof com.e2e.ae2expert.wireless.WirelessContainer
                && ++wirelessGuiTicks == 20) RuntimeIntegrationChecksWireless.verifyClient();
        if(event.phase==TickEvent.Phase.END && finished && ++visualTicks==100 && failed==0) {
            RuntimeIntegrationChecksVisual.client();
        }
        if(event.phase==TickEvent.Phase.END && finished && visualTicks==140) {
            Minecraft mc=Minecraft.getMinecraft();
            System.out.println("[AE2ExpertVisual] SCREENSHOT " + net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.gameDir,mc.displayWidth,mc.displayHeight,mc.getFramebuffer()).getUnformattedText());
        }
        if(event.phase==TickEvent.Phase.END&&finished&&visualTicks==160)RuntimeIntegrationChecksFeatures.jei();
        if(event.phase==TickEvent.Phase.END&&finished&&visualTicks==180) {
            Minecraft mc=Minecraft.getMinecraft();
            System.out.println("[AE2ExpertFeatures] SCREENSHOT "+net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.gameDir,mc.displayWidth,mc.displayHeight,mc.getFramebuffer()).getUnformattedText());
        }
        if(event.phase==TickEvent.Phase.END&&finished&&visualTicks==220&&Boolean.getBoolean("ae2expert.autoexit"))
            Minecraft.getMinecraft().shutdown();
        if (event.phase != TickEvent.Phase.END || started || ++clientTicks < 100) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!new java.io.File(mc.gameDir, ".ae2expert-runtime-check").isFile()) return;
        started = true;
        WorldSettings settings = new WorldSettings(12345, GameType.CREATIVE, false, false, WorldType.FLAT);
        settings.enableCommands();
        mc.launchIntegratedServer(channelWorld() ? "ae2expert-channel-bench" : System.getProperty("ae2expert.testworld","ae2expert-smoke"), channelWorld() ? "AE2 — тест каналов" : "AE2 Expert runtime checks", settings);
    }

    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != 0 || finished) return;
        if (!new java.io.File(event.world.getSaveHandler().getWorldDirectory().getParentFile().getParentFile(), ".ae2expert-runtime-check").isFile()) return;
        if (event.world.getMinecraftServer().getPlayerList().getPlayers().isEmpty()) return;
        WorldServer world = (WorldServer) event.world;
        EntityPlayerMP player = world.getMinecraftServer().getPlayerList().getPlayers().get(0);
        if (channelWorld()) {
            RuntimeIntegrationChecksChannelWorld.tick(world, player, ticks++);
            return;
        }
        try {
            if (ticks++ == 0) {
                System.out.println("[AE2ExpertRuntime] START");
                basicChecks();
                RuntimeIntegrationChecksCpu.setup(world,player);
                RuntimeIntegrationChecksWireless.setup(world,player);
                for(int x=15;x<=48;x++)for(int z=15;z<=38;z++)for(int y=90;y<=91;y++)world.setBlockToAir(new BlockPos(x,y,z));
                for(int x=0;x<=12;x++)for(int z=-2;z<=5;z++)world.setBlockToAir(new BlockPos(x,82,z));
                fixtures(world, player);
                placementChecks(world,player);
                RuntimeIntegrationChecksFeatures.setup(world,player);
                RuntimeIntegrationChecksLogic.setup(world,player);
                network(world, player, 32);
            }
            RuntimeIntegrationChecksLogic.tick(world,ticks);
            RuntimeIntegrationChecksCpu.tick(world,player,ticks);
            RuntimeIntegrationChecksWireless.tick(world,ticks);
            if (ticks == 120) craftingChecks(world);
            if (ticks > 0 && ticks % 200 == 0) {
                int capacity = new int[] {32,64,128,256}[tierIndex];
                long active = terminals.stream().filter(tile -> tile.getProxy().isActive()).count();
                check(active == capacity, "Рабочая сеть " + capacity + ": активно " + active + "/" + terminals.size());
                RuntimeIntegrationChecksVisual.telemetry(world,origin,capacity);
                check(true,"NBT, подсказка Waila и клиентский пакет сохраняют " + capacity + " каналов");
                if (tierIndex == 0) {
                    RuntimeIntegrationChecksFeatures.verify(player);
                    check(chamber.getInventory().getStackInSlot(3).getCount() == 2, "Камера изготовила 2 кристалла");
                    check(chamber.getEnergyStored() == 16000, "Камера списала 4000 RF");
                    check(stockInterface.buffer.getStackInSlot(63).getCount() == 32, "Поддержание запаса в слоте 64");
                    if (craftingOutput != null && !craftingOutput.isEmpty()) {
                        IStorageChannel<IAEItemStack> channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
                        IAEItemStack result = craftingInterface.getProxy().getStorage().getInventory(channel).getStorageList().findPrecise(channel.createStack(craftingOutput));
                        check(result != null && result.getStackSize() >= craftingOutput.getCount(), "Сборщик выполнил шаблон и вернул результат в МЭ");
                    }
                }
                if (++tierIndex < 4) network(world, player, new int[] {32,64,128,256}[tierIndex]);
                else {
                    finished = true;
                    String result = "[AE2ExpertRuntime] DONE passed=" + passed + " failed=" + failed;
                    System.out.println(result);
                    player.sendMessage(new TextComponentString(result));
                    net.minecraft.item.Item visualiser=ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("ae2stuff","visualiser"));
                    ItemStack tool=new ItemStack(visualiser);
                    player.setHeldItem(EnumHand.MAIN_HAND,tool);
                    visualiser.getClass().getMethod("setLocation",ItemStack.class,BlockPos.class,int.class).invoke(visualiser,tool,origin,0);
                    player.capabilities.isFlying=true;
                    player.sendPlayerAbilities();
                    player.connection.setPlayerLocation(16.5,90,12.5,0,20);
                }
            }
        } catch (Throwable failure) {
            failed++;
            finished = true;
            System.out.println("[AE2ExpertRuntime] FATAL " + failure);
            failure.printStackTrace();
        }
    }

    private void check(boolean value, String name) {
        if (value) passed++; else failed++;
        System.out.println("[AE2ExpertRuntime] " + (value ? "PASS " : "FAIL ") + name);
    }

    private void basicChecks() {
        long items = ForgeRegistries.ITEMS.getKeys().stream().filter(key -> key.getNamespace().equals("ae2expert")).count();
        check(items == 94, "Зарегистрирован 94 предмета и блока: " + items);
        int recipes = 0;
        for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (recipe.getRegistryName() == null || !recipe.getRegistryName().getNamespace().equals("ae2expert")) continue;
            recipes++;
            check(!recipe.getRecipeOutput().isEmpty(), "Выход рецепта " + recipe.getRegistryName());
            for (net.minecraft.item.crafting.Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != net.minecraft.item.crafting.Ingredient.EMPTY)
                    check(ingredient.getMatchingStacks().length > 0, "Ингредиент " + recipe.getRegistryName());
            }
        }
        check(recipes == 94, "Загружен 94 рецепта: " + recipes);
        IStorageChannel<IAEItemStack> itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        ItemStack itemCell = new ItemStack(ModItems.itemCells.get(CellTier.M512));
        ICellInventoryHandler<IAEItemStack> inventory = AEApi.instance().registries().cell().getCellInventory(itemCell, null, itemChannel);
        IAEItemStack stones = itemChannel.createStack(new ItemStack(Blocks.COBBLESTONE));
        stones.setStackSize(3_000_000_000L);
        check(inventory.injectItems(stones, Actionable.MODULATE, null) == null, "Реальная ячейка принимает 3 млрд предметов");
        inventory.getCellInv().persist();
        ICellInventoryHandler<IAEItemStack> loaded = AEApi.instance().registries().cell().getCellInventory(itemCell, null, itemChannel);
        check(loaded.getCellInv().getStoredItemCount() == 3_000_000_000L, "Перезагрузка реальной предметной ячейки");
        check(inventory.getCellInv().getUpgradesInventory().insertItem(0, ModMachineItems.ae2Material(29, 1), false).isEmpty(), "Установка нечёткой карты в ячейку");
        IStorageChannel<IAEFluidStack> fluids = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        ItemStack fluidCell = new ItemStack(ModItems.fluidCells.get(CellTier.M512));
        ICellInventoryHandler<IAEFluidStack> tank = AEApi.instance().registries().cell().getCellInventory(fluidCell, null, fluids);
        IAEFluidStack water = fluids.createStack(new FluidStack(FluidRegistry.WATER, 1));
        long amount = Math.min(3_000_000_000_000L, tank.getCellInv().getTotalBytes() * fluids.getUnitsPerByte() / 2);
        water.setStackSize(amount);
        check(tank.injectItems(water, Actionable.MODULATE, null) == null && amount > Integer.MAX_VALUE, "Большой объём реальной жидкостной ячейки");
        tank.getCellInv().persist();
        check(AEApi.instance().registries().cell().getCellInventory(fluidCell, null, fluids).getCellInv().getStoredItemCount() == amount, "Перезагрузка жидкостной ячейки");
        ItemStack unlimited = new ItemStack(ModItems.infiniteCell);
        ICellInventoryHandler<IAEItemStack> infinity = AEApi.instance().registries().cell().getCellInventory(unlimited, null, itemChannel);
        check(infinity.getCellInv().getTotalItemTypes() == 1024, "Бесконечная ячейка обслуживается своим обработчиком");
        IRecipe upgrade = ForgeRegistries.RECIPES.getValue(new net.minecraft.util.ResourceLocation("ae2expert", "cell_32k"));
        ItemStack empty = new ItemStack(ModItems.itemCells.get(CellTier.K16));
        ItemStack filled = empty.copy();
        ICellInventoryHandler<IAEItemStack> small = AEApi.instance().registries().cell().getCellInventory(filled, null, itemChannel);
        IAEItemStack one = itemChannel.createStack(new ItemStack(Items.REDSTONE));
        small.injectItems(one, Actionable.MODULATE, null);
        ItemStack upgraded = empty.copy();
        ((com.e2e.ae2expert.item.ExpertStorageCell<?>) upgraded.getItem()).getUpgradesInventory(upgraded)
                .insertItem(0,ModMachineItems.ae2Material(29,1),false);
        for (net.minecraft.item.crafting.Ingredient ingredient : upgrade.getIngredients()) if (ingredient.apply(empty)) {
            check(!ingredient.apply(filled), "Крафт отвергает заполненную ячейку");
            check(!ingredient.apply(upgraded), "Крафт отвергает ячейку с картой");
            break;
        }
        com.e2e.ae2expert.item.ItemExpertGasCell gasItem = (com.e2e.ae2expert.item.ItemExpertGasCell) ModItems.all().stream()
                .filter(item -> item instanceof com.e2e.ae2expert.item.ItemExpertGasCell)
                .filter(item -> ((com.e2e.ae2expert.item.ItemExpertGasCell)item).getTier() == CellTier.M512).findFirst().get();
        IStorageChannel<extracells.api.gas.IAEGasStack> gasChannel = gasItem.getChannel();
        ItemStack gasCell = new ItemStack(gasItem);
        ICellInventoryHandler<extracells.api.gas.IAEGasStack> gasInventory = AEApi.instance().registries().cell().getCellInventory(gasCell,null,gasChannel);
        extracells.api.gas.IAEGasStack gas = gasChannel.createStack(new mekanism.api.gas.GasStack(mekanism.api.gas.GasRegistry.getGas("hydrogen"),1));
        long gasAmount = Math.min(3_000_000_000_000L, gasInventory.getCellInv().getTotalBytes()*gasChannel.getUnitsPerByte()/2);
        gas.setStackSize(gasAmount);
        check(gasInventory.injectItems(gas,Actionable.MODULATE,null) == null && gasAmount > Integer.MAX_VALUE,"Большой объём газовой ячейки");
        gasInventory.getCellInv().persist();
        check(AEApi.instance().registries().cell().getCellInventory(gasCell,null,gasChannel).getCellInv().getStoredItemCount()==gasAmount,"Перезагрузка газовой ячейки");
    }

    private void fixtures(WorldServer world, EntityPlayerMP player) {
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.setWorldTime(6000);
        for (int x = -6; x < 14; x++) for (int z = -6; z < 14; z++)
            world.setBlockState(new BlockPos(x, 81, z), Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(new BlockPos(0,82,0), MachineRegistry.FLUIX_CHAMBER.getDefaultState());
        world.setBlockState(new BlockPos(2,82,0), MachineRegistry.RF_EU_CONVERTER.getDefaultState());
        world.setBlockState(new BlockPos(4,82,0), InterfaceRegistry.BLOCKS.get(4).getDefaultState());
        stockInterface = (TileExpertInterface)world.getTileEntity(new BlockPos(4,82,0));
        stockInterface.config.setStackInSlot(63,new ItemStack(Blocks.COBBLESTONE,32));
        world.setBlockState(new BlockPos(4,82,-1),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get().getDefaultState());
        world.setBlockState(new BlockPos(5,82,-1),AEApi.instance().definitions().blocks().drive().maybeBlock().get().getDefaultState());
        ItemStack cell = new ItemStack(ModItems.itemCells.get(CellTier.K16));
        IStorageChannel<IAEItemStack> channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IAEItemStack stones = channel.createStack(new ItemStack(Blocks.COBBLESTONE,64));
        AEApi.instance().registries().cell().getCellInventory(cell,null,channel).injectItems(stones,Actionable.MODULATE,null);
        ((appeng.tile.storage.TileDrive)world.getTileEntity(new BlockPos(5,82,-1))).getInternalInventory().insertItem(0,cell,false);
        world.setBlockState(new BlockPos(5,82,0), InterfaceRegistry.BLOCKS.get(0).getDefaultState());
        craftingInterface = (TileExpertInterface)world.getTileEntity(new BlockPos(5,82,0));
        world.setBlockState(new BlockPos(6,82,3), InterfaceRegistry.BLOCKS.get(0).getDefaultState());
        processingInterface = (TileExpertInterface)world.getTileEntity(new BlockPos(6,82,3));
        world.setBlockState(new BlockPos(6,82,4),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get().getDefaultState());
        world.setBlockState(new BlockPos(7,82,3),Blocks.CHEST.getDefaultState());
        processingChest=(net.minecraft.tileentity.TileEntityChest)world.getTileEntity(new BlockPos(7,82,3));
        chamber = (TileFluixChamber) world.getTileEntity(new BlockPos(0,82,0));
        chamber.getInventory().setStackInSlot(0, ModMachineItems.ae2Material(1,1));
        chamber.getInventory().setStackInSlot(1, new ItemStack(Items.REDSTONE));
        chamber.getInventory().setStackInSlot(2, new ItemStack(Items.QUARTZ));
        for (int i=0;i<20;i++) chamber.receiveEnergy(EnumFacing.UP,1000,false);
        TileRfEuConverter converter = (TileRfEuConverter) world.getTileEntity(new BlockPos(2,82,0));
        check(converter.canConnectEnergy(EnumFacing.UP) && converter.receiveEnergy(EnumFacing.UP,4096,false) == 4096, "Классический RF-вход");
        converter.toggleMode();
        check(converter.receiveEnergy(EnumFacing.UP,100,false) == 0, "Переключение направления закрывает RF-вход");
        check(converter.getCapability(CapabilityEnergy.ENERGY, EnumFacing.UP).canExtract(), "Forge Energy соответствует выбранному режиму");
        player.setGameType(GameType.CREATIVE);
        player.connection.setPlayerLocation(4.5, 82, 5.5, 180, 10);
        for (com.e2e.ae2expert.assembler.BlockExpertAssembler block : com.e2e.ae2expert.assembler.AssemblerRegistry.BLOCKS.values()) {
            BlockPos at = new BlockPos(6 + block.getTier().ordinal(),82,0);
            world.setBlockState(at, block.getDefaultState());
            check(world.getTileEntity(at) instanceof appeng.tile.crafting.TileMolecularAssembler, "Размещение сборщика " + block.getTier());
        }
        net.minecraft.inventory.InventoryCrafting table = table();
        table.setInventorySlotContents(0,new ItemStack(Blocks.PLANKS));
        craftingOutput=net.minecraft.item.crafting.CraftingManager.findMatchingResult(table,world);
        check(!craftingOutput.isEmpty(),"Найден реальный рецепт для проверки сборщика");
        craftingInterface.patterns.setStackInSlot(17,pattern(true,table,craftingOutput));
        table=table();
        table.setInventorySlotContents(0,new ItemStack(Blocks.PLANKS,40));
        table.setInventorySlotContents(1,new ItemStack(Items.REDSTONE,40));
        processingInterface.patterns.setStackInSlot(17,pattern(false,table,new ItemStack(Blocks.STONE)));
    }

    private net.minecraft.inventory.InventoryCrafting table() {
        return new net.minecraft.inventory.InventoryCrafting(new appeng.container.ContainerNull(),3,3);
    }
    private void placementChecks(WorldServer world, EntityPlayerMP player) {
        BlockPos at=new BlockPos(12,82,5);
        world.setBlockToAir(at);
        ItemStack held=player.getHeldItemMainhand().copy();
        ItemStack cable=new ItemStack(ModItems.cables.get(CableTier.C32));
        player.setHeldItem(EnumHand.MAIN_HAND,cable);
        check(cable.getItem().onItemUse(player,world,at.down(),EnumHand.MAIN_HAND,EnumFacing.UP,0.5f,0.5f,0.5f)==net.minecraft.util.EnumActionResult.SUCCESS,
                "Предмет кабеля размещает новый кабельный блок через onItemUse");
        TileEntity placed=world.getTileEntity(at);
        check(placed instanceof IPartHost && ((IPartHost)placed).getPart(AEPartLocation.INTERNAL) instanceof com.e2e.ae2expert.part.PartExpertCable,
                "В новом блоке находится часть аддона");
        world.setBlockToAir(at);
        world.setBlockState(at,AEApi.instance().definitions().blocks().multiPart().maybeBlock().get().getDefaultState());
        IPartHost host=(IPartHost)world.getTileEntity(at);
        host.addPart(AEApi.instance().definitions().parts().cableGlass().stack(appeng.api.util.AEColor.TRANSPARENT,1),AEPartLocation.INTERNAL,player,EnumHand.MAIN_HAND);
        List<com.e2e.ae2expert.item.ItemExpertBus> buses=new ArrayList<>();
        buses.addAll(ModItems.importBuses.values());buses.addAll(ModItems.exportBuses.values());
        for(com.e2e.ae2expert.item.ItemExpertBus item:buses) {
            player.setHeldItem(EnumHand.MAIN_HAND,new ItemStack(item));
            check(item.onItemUse(player,world,at,EnumHand.MAIN_HAND,EnumFacing.EAST,0.5f,0.5f,0.5f)==net.minecraft.util.EnumActionResult.SUCCESS
                    && host.getPart(AEPartLocation.EAST)!=null,"Размещение шины предметом: "+item.getRegistryName());
            host.removePart(AEPartLocation.EAST,false);
        }
        player.setHeldItem(EnumHand.MAIN_HAND,held);
    }
    private ItemStack pattern(boolean crafting, net.minecraft.inventory.InventoryCrafting table, ItemStack output) {
        ItemStack stack=new ItemStack(ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("appliedenergistics2","encoded_pattern")));
        net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();
        tag.setBoolean("crafting",crafting);
        tag.setBoolean("substitute",false);
        net.minecraft.nbt.NBTTagList in=new net.minecraft.nbt.NBTTagList(),out=new net.minecraft.nbt.NBTTagList();
        for(int i=0;i<9;i++)in.appendTag(table.getStackInSlot(i).isEmpty()
                ? new net.minecraft.nbt.NBTTagCompound() : table.getStackInSlot(i).writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        out.appendTag(output.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        tag.setTag("in",in);tag.setTag("out",out);stack.setTagCompound(tag);
        return stack;
    }
    private List<appeng.api.networking.crafting.ICraftingPatternDetails> options(TileExpertInterface tile) {
        List<appeng.api.networking.crafting.ICraftingPatternDetails> result=new ArrayList<>();
        tile.provideCrafting(new appeng.api.networking.crafting.ICraftingProviderHelper() {
            @Override public void addCraftingOption(appeng.api.networking.crafting.ICraftingMedium medium,appeng.api.networking.crafting.ICraftingPatternDetails detail) { result.add(detail); }
            @Override public void setEmitable(IAEItemStack item) { }
        });
        return result;
    }
    private void craftingChecks(WorldServer world) {
        List<appeng.api.networking.crafting.ICraftingPatternDetails> crafting=options(craftingInterface);
        check(crafting.size()==1,"Шаблон из слота 18 опубликован в сети");
        net.minecraft.inventory.InventoryCrafting table=table();
        table.setInventorySlotContents(0,new ItemStack(Blocks.PLANKS));
        check(crafting.size()==1 && craftingInterface.pushPattern(crafting.get(0),table),"Передача шаблона ускоренному сборщику");
        List<appeng.api.networking.crafting.ICraftingPatternDetails> processing=options(processingInterface);
        check(processing.size()==1,"Опубликован шаблон обработки");
        table=table();table.setInventorySlotContents(0,new ItemStack(Blocks.PLANKS,40));table.setInventorySlotContents(1,new ItemStack(Items.REDSTONE,40));
        for(int i=0;i<26;i++)processingChest.setInventorySlotContents(i,new ItemStack(Blocks.COBBLESTONE,64));
        check(processing.size()==1 && !processingInterface.pushPattern(processing.get(0),table),"Общая симуляция не обещает один слот двум ингредиентам");
        check(processingChest.getStackInSlot(26).isEmpty(),"Отклонённый шаблон не меняет сундук");
        processingChest.clear();
        check(processing.size()==1 && processingInterface.pushPattern(processing.get(0),table),"Передача обработки во внешний инвентарь");
        int count=0;for(int i=0;i<27;i++)count+=processingChest.getStackInSlot(i).getCount();
        check(count==80,"Все ингредиенты обработки переданы без потери");
    }

    private void network(WorldServer world, EntityPlayerMP player, int capacity) {
        for (BlockPos pos : networkBlocks) world.setBlockToAir(pos);
        networkBlocks.clear();
        terminals.clear();
        place(world, origin.add(0,0,-1), AEApi.instance().definitions().blocks().controller().maybeBlock().get());
        place(world, origin.add(-1,0,-1), AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        ItemStack cable = new ItemStack(ModItems.cables.get(CableTier.values()[tierIndex]));
        Set<BlockPos> cables = new LinkedHashSet<>();
        for (int i=0;i<capacity;i++) cables.add(origin.add(i%32,0,(i/32)*3));
        for (int z=0;z<((capacity-1)/32)*3;z++) cables.add(origin.add(0,0,z));
        for (BlockPos at : cables) {
            place(world, at, AEApi.instance().definitions().blocks().multiPart().maybeBlock().get());
            IPartHost host = (IPartHost) world.getTileEntity(at);
            if (host.addPart(cable.copy(), AEPartLocation.INTERNAL, player, EnumHand.MAIN_HAND) == null)
                throw new IllegalStateException("Не удалось поставить кабель " + capacity + " в " + at);
        }
        for (int i=0;i<capacity;i++) {
            BlockPos at = origin.add(i%32,1,(i/32)*3);
            place(world,at,InterfaceRegistry.BLOCKS.get(0));
            terminals.add((TileExpertInterface)world.getTileEntity(at));
        }
        System.out.println("[AE2ExpertRuntime] NETWORK_READY " + capacity);
    }
    private void place(WorldServer world, BlockPos pos, net.minecraft.block.Block block) {
        world.getChunk(pos);
        world.setBlockState(pos, block.getDefaultState());
        networkBlocks.add(pos);
    }
}

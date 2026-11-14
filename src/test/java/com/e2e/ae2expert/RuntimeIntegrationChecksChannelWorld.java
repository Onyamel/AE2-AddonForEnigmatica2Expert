package com.e2e.ae2expert;

import java.util.*;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.parts.IPartHost;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.util.*;
import appeng.tile.storage.TileDrive;
import com.e2e.ae2expert.cable.CableTier;
import com.e2e.ae2expert.interfacepkg.*;
import com.e2e.ae2expert.storage.CellTier;
import com.e2e.ae2expert.wireless.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

final class RuntimeIntegrationChecksChannelWorld {
    private static final int Y = 70;
    private static final int[] LIMITS = {32,64,128,256,256};
    private static final List<List<BlockPos>> LOAD = new ArrayList<>();
    private static final List<BlockPos> EXTRA = new ArrayList<>();
    private static final List<BlockPos[]> RADIOS = new ArrayList<>();

    private static void set(WorldServer w, BlockPos p, Block b) { w.setBlockState(p,b.getDefaultState(),3); }
    private static BlockPos p(int x, int z) { return new BlockPos(x,Y,z); }
    private static void sign(WorldServer w, BlockPos p, String... lines) {
        set(w,p,Blocks.STANDING_SIGN);
        TileEntitySign s=(TileEntitySign)w.getTileEntity(p);
        for(int i=0;i<lines.length;i++) s.signText[i]=new TextComponentString(lines[i]);
        s.markDirty();
    }
    private static void cable(WorldServer w, EntityPlayerMP player, BlockPos at) {
        set(w,at,AEApi.instance().definitions().blocks().multiPart().maybeBlock().get());
        IPartHost host=(IPartHost)w.getTileEntity(at);
        if(host.addPart(new ItemStack(ModItems.cables.get(CableTier.C256)),AEPartLocation.INTERNAL,
                player,EnumHand.MAIN_HAND)==null) throw new IllegalStateException("Cable " + at);
    }
    private static void button(WorldServer w, BlockPos at, BlockPos target, boolean add) {
        set(w,at,Blocks.COMMAND_BLOCK);
        TileEntityCommandBlock cmd=(TileEntityCommandBlock)w.getTileEntity(at);
        cmd.getCommandBlockLogic().setCommand("setblock "+target.getX()+" "+target.getY()+" "+target.getZ()+" "+
                (add ? InterfaceRegistry.BLOCKS.get(0).getRegistryName() : "minecraft:air"));
        cmd.markDirty();
        set(w,at.up(),Blocks.STONE_PRESSURE_PLATE);
        sign(w,at.south(),add ? "+1 интерфейс" : "Убрать +1","Встань на плиту");
    }
    private static void arm(WorldServer w, EntityPlayerMP player, int z, int relayX, int tier, int n, int direction, List<BlockPos> nodes) {
        BlockPos relay=p(relayX,z);
        set(w,relay,WirelessRegistry.BLOCKS.get(tier));
        for(int row=0;row<n/32;row++) {
            int rz=z+direction*(2+row*2);
            for(int x=relayX;x<relayX+32;x++) {
                BlockPos at=p(x,rz);
                cable(w,player,at);
                set(w,at.up(),InterfaceRegistry.BLOCKS.get(0));
                nodes.add(at.up());
            }
        }
        for(int i=1;i<=n/16;i++) if(i%2==1) cable(w,player,p(relayX,z+i*direction));
    }
    private static void build(WorldServer w, EntityPlayerMP player) {
        w.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        w.getGameRules().setOrCreateGameRule("doDaylightCycle","false");
        w.setWorldTime(6000);
        player.setGameType(net.minecraft.world.GameType.CREATIVE);
        for(int x=-5;x<=56;x++) for(int z=-10;z<=144;z++) set(w,new BlockPos(x,Y-1,z),Blocks.STONE);
        for(int i=0;i<LIMITS.length;i++) {
            int z=i==4 ? 124 : 10+i*25, limit=LIMITS[i];
            List<BlockPos> nodes=new ArrayList<>();
            LOAD.add(nodes);
            BlockPos hub=p(11,z), first=p(23,z);
            set(w,hub,WirelessRegistry.HUB);
            set(w,hub.west(),AEApi.instance().definitions().blocks().controller().maybeBlock().get());
            set(w,hub.west(2),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
            if(i<4) {
                arm(w,player,z,23,i+1,limit,1,nodes);
                RADIOS.add(new BlockPos[]{hub,first});
            } else {
                arm(w,player,z,23,4,128,1,nodes);
                arm(w,player,z-2,23,4,128,-1,nodes);
                RADIOS.add(new BlockPos[]{hub,first,p(23,z-2)});
            }
            cable(w,player,first.east());
            EXTRA.add(first.east().up());
            button(w,p(13,z+1),EXTRA.get(i),true);
            button(w,p(16,z+1),EXTRA.get(i),false);
            sign(w,p(14,z+3),i<4 ? "Передатчик "+limit : "Центр: 2 × 128","Лимит "+limit,"Плиты: +1 / сброс");
        }
        BlockPos hub=p(11,-4), relay=p(23,-4);
        set(w,hub,WirelessRegistry.HUB);
        set(w,hub.west(),AEApi.instance().definitions().blocks().controller().maybeBlock().get());
        set(w,hub.west(2),AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        set(w,hub.west().north(),AEApi.instance().definitions().blocks().drive().maybeBlock().get());
        TileDrive drive=(TileDrive)w.getTileEntity(hub.west().north());
        ItemStack cell=new ItemStack(ModItems.itemCells.get(CellTier.K16));
        IItemStorageChannel channel=AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        AEApi.instance().registries().cell().getCellInventory(cell,null,channel).injectItems(
                channel.createStack(new ItemStack(Items.DIAMOND,32)),Actionable.MODULATE,null);
        drive.getInternalInventory().insertItem(0,cell,false);
        set(w,relay,WirelessRegistry.BLOCKS.get(1));
        cable(w,player,relay.east());
        IPartHost terminal=(IPartHost)w.getTileEntity(relay.east());
        if(terminal.addPart(AEApi.instance().definitions().parts().terminal().maybeStack(1).get(),
                AEPartLocation.SOUTH,player,EnumHand.MAIN_HAND)==null) throw new IllegalStateException("Terminal");
        RADIOS.add(new BlockPos[]{hub,relay});
        sign(w,p(22,-2),"МЭ-терминал","В сети 32 алмаза");
        sign(w,p(5,2),"Стенды AE2","32 / 64 / 128","256 / центр","Вдоль +Z");
        w.setSpawnPoint(p(5,0));
        player.connection.setPlayerLocation(5.5,Y+1,0.5,0,0);
        player.inventory.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,16));
    }

    private static void pair(WorldServer w, EntityPlayerMP player) {
        ItemStack previous=player.getHeldItemMainhand();
        player.setHeldItem(EnumHand.MAIN_HAND,new ItemStack(WirelessRegistry.LINKER));
        try {
            for(BlockPos[] row:RADIOS) for(BlockPos position:row) WirelessRegistry.LINKER.onItemUse(
                    player,w,position,EnumHand.MAIN_HAND,EnumFacing.UP,.5f,1,.5f);
        } finally {player.setHeldItem(EnumHand.MAIN_HAND,previous);}
    }

    private static void verify(WorldServer w, boolean overflow) {
        for(int i=0;i<LOAD.size();i++) {
            int active=0;
            List<BlockPos> nodes=new ArrayList<>(LOAD.get(i));
            if(overflow) nodes.add(EXTRA.get(i));
            for(BlockPos position:nodes) {
                TileEntity tile=w.getTileEntity(position);
                if(tile instanceof TileExpertInterface && ((TileExpertInterface)tile).getProxy().isActive()) active++;
            }
            System.out.println("[ChannelWorld] station="+i+" overload="+overflow+" active="+active+"/"+nodes.size());
            if(active!=LIMITS[i]) throw new IllegalStateException("Station "+i+" should carry "+LIMITS[i]+" channels");
        }
    }

    static void tick(WorldServer w, EntityPlayerMP player, int tick) {
        if(tick==0) build(w,player);
        if(tick==60) pair(w,player);
        if(tick==220) {
            verify(w,false);
            for(BlockPos at:EXTRA) set(w,at,InterfaceRegistry.BLOCKS.get(0));
        }
        if(tick==420) {
            verify(w,true);
            for(BlockPos at:EXTRA) w.setBlockToAir(at);
        }
        if(tick==620) {
            verify(w,false);
            System.out.println("[ChannelWorld] DONE");
            w.getMinecraftServer().initiateShutdown();
        }
    }
}

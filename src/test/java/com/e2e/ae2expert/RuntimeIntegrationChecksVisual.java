package com.e2e.ae2expert;

import appeng.api.parts.*;
import appeng.api.util.AEPartLocation;
import com.e2e.ae2expert.part.PartExpertCable;
import com.e2e.ae2expert.cable.CableTier;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Проверки работают только в отдельном профиле вместе с тестовым модом. */
public final class RuntimeIntegrationChecksVisual {
    public static void telemetry(World world,BlockPos pos,int capacity)throws Exception {
        IPartHost host=(IPartHost)world.getTileEntity(pos);
        PartExpertCable cable=(PartExpertCable)host.getPart(AEPartLocation.INTERNAL);
        NBTTagCompound tag=new NBTTagCompound();cable.writeToNBT(tag);
        require(tag.getInteger("usedChannels")==capacity,"NBT: " + tag);
        require(tag.getInteger("expertChannelCapacity")==capacity,"Ёмкость NBT");
        io.netty.buffer.ByteBuf packet=io.netty.buffer.Unpooled.buffer();
        try {
            cable.writeToStream(packet);
            PartExpertCable received=new PartExpertCable(new ItemStack(ModItems.cables.get(CableTier.values()[Integer.numberOfTrailingZeros(capacity)-5])));
            received.setPartHostInfo(AEPartLocation.INTERNAL,host,(net.minecraft.tileentity.TileEntity)host);
            received.readFromStream(packet);
            require(!packet.isReadable(),"Пакет полностью прочитан");
            require(received.getDisplayedUsedChannels()==capacity,"Клиентский счётчик " + capacity);
        } finally {packet.release();}
        Class<?> type=Class.forName("appeng.integration.modules.waila.part.ChannelWailaDataProvider");
        Object provider=type.getConstructor().newInstance();
        Method nbt=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals("getNBTData")).findFirst().get();
        NBTTagCompound tooltipTag=(NBTTagCompound)nbt.invoke(provider,null,cable,null,new NBTTagCompound(),null,null);
        Class<?> accessor=Class.forName("mcp.mobius.waila.api.IWailaDataAccessor");
        Object access=Proxy.newProxyInstance(accessor.getClassLoader(),new Class<?>[]{accessor},(proxy,method,args)->method.getName().equals("getNBTData")?tooltipTag:null);
        Method body=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals("getWailaBody")).findFirst().get();
        List<String> lines=new ArrayList<>();body.invoke(provider,cable,lines,access,null);
        require(lines.size()==1 && lines.get(0).contains(capacity+""),"Подсказка Waila: " + lines);
        System.out.println("[AE2ExpertVisual] WAILA " + capacity + " " + lines);
    }
    public static void client() throws Exception {
        Minecraft mc=Minecraft.getMinecraft();
        try {
            Field cache=Class.forName("appeng.client.render.cablebus.CableBusBakedModel").getDeclaredField("CABLE_MODEL_CACHE");
            cache.setAccessible(true);
            require(cache.get(null) instanceof ConcurrentMap,"Настоящий кэш AE2 потокобезопасен");
            List<IBlockState> states=new ArrayList<>();
            for(int z=16;z<=37;z+=3)for(int x=16;x<=47;x++) {
                BlockPos pos=new BlockPos(x,90,z);IBlockState state=mc.world.getBlockState(pos);
                if(mc.world.getTileEntity(pos) instanceof IPartHost) {
                    IBlockState extended=state.getBlock().getExtendedState(state,mc.world,pos);
                    appeng.client.render.cablebus.CableBusRenderState render=((net.minecraftforge.common.property.IExtendedBlockState)extended)
                            .getValue(appeng.block.networking.BlockCableBus.RENDER_STATE_PROPERTY);
                    if(render.getCableType()!=appeng.api.util.AECableType.NONE)states.add(extended);
                }
            }
            require(!states.isEmpty(),"Кабели тестовой сети загружены клиентом");
            // Поиск в реестре моделей требует чистого состояния; расширенные данные
            // передаются уже самой модели, как это делает обычный рендер Minecraft.
            IBlockState clean=((net.minecraftforge.common.property.IExtendedBlockState)states.get(0)).getClean();
            IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(clean);
            System.out.println("[AE2ExpertVisual] MODEL " + model.getClass().getName()+" states="+states.size());
            require(model.getClass().getName().equals("appeng.client.render.cablebus.CableBusBakedModel"),"Получена настоящая модель AE2");
            ((Map<?,?>)cache.get(null)).clear();
            ExecutorService workers=Executors.newFixedThreadPool(4);
            try {
                List<Future<Integer>> results=new ArrayList<>();
                for(int worker=0;worker<4;worker++)results.add(workers.submit(()->{
                    net.minecraftforge.client.ForgeHooksClient.setRenderLayer(net.minecraft.util.BlockRenderLayer.CUTOUT);
                    require(net.minecraftforge.client.MinecraftForgeClient.getRenderLayer()==net.minecraft.util.BlockRenderLayer.CUTOUT,"Слой рендера CUTOUT установлен для потока");
                    try {int count=0;for(int loop=0;loop<8;loop++)for(IBlockState state:states) {
                        List<BakedQuad> quads=model.getQuads(state,null,0);
                        require(!quads.isEmpty(),"Пустая геометрия: "+model.getClass().getName()+" state="+state);count++;
                    }return count;} finally {net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);}
                }));
                int count=0;for(Future<Integer> result:results)count+=result.get(30,TimeUnit.SECONDS);
                System.out.println("[AE2ExpertVisual] RENDER_STRESS PASS calls="+count+" threads=4");
            } finally {workers.shutdownNow();}
            net.minecraftforge.client.ForgeHooksClient.setRenderLayer(net.minecraft.util.BlockRenderLayer.CUTOUT);
            try {
                int accent=com.e2e.ae2expert.client.VisualPalette.shade(com.e2e.ae2expert.client.VisualPalette.cable(256),0.95);
                int packed=(accent&255)<<16|(accent&0xff00)|(accent>>16&255);
                require(hasColor(model.getQuads(states.get(0),null,0),packed),"Индикаторы действительно получили приглушённый цвет тира");
                boolean originalGray=false;
                for(Object value:((Map<?,?>)cache.get(null)).values())
                    if(hasColor((List<BakedQuad>)value,0xC9C9C9)){originalGray=true;break;}
                require(originalGray,"Исходные грани кэша не перекрашены и не повреждены");
                System.out.println("[AE2ExpertVisual] PALETTE PASS original_cache_unchanged=true");
            } finally {net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);}
            System.out.println("[AE2ExpertVisual] SCREENSHOT " + net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.gameDir,mc.displayWidth,mc.displayHeight,mc.getFramebuffer()).getUnformattedText());
            System.out.println("[AE2ExpertVisual] DONE failed=0");
        } catch(Throwable failure) {
            System.out.println("[AE2ExpertVisual] FATAL " + failure);failure.printStackTrace();
        }
    }
    private static boolean hasColor(List<BakedQuad> quads,int color) {
        for(BakedQuad quad:quads) {
            int offset=quad.getFormat().getColorOffset();if(offset<0)continue;
            for(int i=offset/4;i<quad.getVertexData().length;i+=quad.getFormat().getIntegerSize())
                if((quad.getVertexData()[i]&0xffffff)==color)return true;
        }return false;
    }
    private static void require(boolean value,String text) {if(!value)throw new AssertionError(text);}
}

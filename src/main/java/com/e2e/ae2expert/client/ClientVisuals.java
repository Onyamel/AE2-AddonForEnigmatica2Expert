package com.e2e.ae2expert.client;

import java.util.ArrayList;
import java.util.List;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.block.networking.BlockCableBus;
import appeng.client.render.cablebus.CableBusRenderState;
import com.e2e.ae2expert.part.PartExpertCable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.lwjgl.opengl.GL11;

/** Только клиентские цвета: физические цвета сети и исходные кэшированные грани не меняются. */
public final class ClientVisuals {
    private ClientVisuals() { }
    public static List<BakedQuad> tintCableQuads(List<BakedQuad> original, IBlockState state) {
        if(original.isEmpty()||!(state instanceof IExtendedBlockState))return original;
        CableBusRenderState render=((IExtendedBlockState)state).getValue(BlockCableBus.RENDER_STATE_PROPERTY);
        if(render==null||render.getPos()==null||render.getWorld()==null)return original;
        TileEntity tile=render.getWorld().getTileEntity(render.getPos());
        if(!(tile instanceof IPartHost))return original;
        Object part=((IPartHost)tile).getPart(AEPartLocation.INTERNAL);
        if(!(part instanceof PartExpertCable))return original;
        int accent=VisualPalette.cable(((PartExpertCable)part).getChannelCapacity());
        int paint = ((PartExpertCable)part).getPaint();
        if (paint >= 0) accent = paint;
        List<BakedQuad> result=new ArrayList<>(original.size());
        for(BakedQuad quad:original) {
            int[] copy=null;
            int offsetBytes=quad.getFormat().getColorOffset();
            int offset=offsetBytes/4, stride=quad.getFormat().getIntegerSize();
            if(offsetBytes>=0 && offsetBytes%4==0 && stride>0 && offset<stride
                    && quad.getVertexData().length%stride==0)for(int i=offset;i<quad.getVertexData().length;i+=stride) {
                int before=quad.getVertexData()[i], rgb=before&0xffffff;
                // Эти два серых оттенка задаёт CableBuilder для полос индикатора.
                if(rgb!=0x7C7C7C&&rgb!=0xC9C9C9)continue;
                if(copy==null)copy=quad.getVertexData().clone();
                int color=VisualPalette.shade(accent,rgb==0x7C7C7C?0.50:0.95);
                copy[i]=(before&0xff000000)|(color&255)<<16|(color&0xff00)|(color>>16&255);
            }
            result.add(copy==null?quad:new BakedQuad(copy,quad.getTintIndex(),quad.getFace(),quad.getSprite(),quad.shouldApplyDiffuseLighting(),quad.getFormat()));
        }
        return result;
    }
    public static void quietColor3f(float r,float g,float b) { quietColor3d(r,g,b); }
    public static void quietColor3d(double r,double g,double b) {
        double strength=Math.max(r,Math.max(g,b));
        if(strength<=0){GL11.glColor3d(0,0,0);return;}
        boolean red=r>strength*0.5,green=g>strength*0.5,blue=b>strength*0.5;
        int tone=red&&green&&!blue?0xB1A077:blue&&!red&&!green?0x718FA7:red&&blue&&!green?0xA18BAA:red&&!green&&!blue?0xB07973:0xADB4B9;
        GL11.glColor3d((tone>>16&255)/255.0*strength,(tone>>8&255)/255.0*strength,(tone&255)/255.0*strength);
    }
}

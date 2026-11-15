package appeng.client.render.cablebus;

import appeng.api.parts.IPartHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import com.e2e.ae2expert.client.VisualPalette;
import com.e2e.ae2expert.part.PartExpertCable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

/** Builds only the center cable quads. AE2 still renders the attachments and facades. */
public final class ExpertThinCable {
    private ExpertThinCable() { }

    public static List<BakedQuad> replace(List<BakedQuad> original, CableBuilder builder, CableBusRenderState state) {
        if (state.getCableType() != AECableType.DENSE_SMART || state.getPos() == null || original.isEmpty()) return original;
        IBlockAccess world = state.getWorld();
        TileEntity tile = world == null ? null : world.getTileEntity(state.getPos());
        if (!(tile instanceof IPartHost)) return original;
        Object installed = ((IPartHost) tile).getPart(AEPartLocation.INTERNAL);
        if (!(installed instanceof PartExpertCable)) return original;
        PartExpertCable cable = (PartExpertCable) installed;
        int color = cable.getPaint() >= 0 ? cable.getPaint() : VisualPalette.cable(cable.getChannelCapacity());
        List<BakedQuad> shell = new ArrayList<>();
        Map<EnumFacing, AECableType> links = state.getConnectionTypes();
        EnumFacing first = links.isEmpty() ? null : links.keySet().iterator().next();
        boolean straight = links.size() == 2 && links.containsKey(first.getOpposite())
                && state.getAttachmentConnections().isEmpty() && state.getAttachments().isEmpty();
        if (straight) {
            builder.addStraightGlassConnection(first, AEColor.WHITE, shell);
        } else {
            builder.addCableCore(CableCoreType.GLASS, AEColor.WHITE, shell);
            for (Map.Entry<EnumFacing, AECableType> link : links.entrySet())
                builder.addGlassConnection(link.getKey(), AEColor.WHITE, link.getValue(),
                        state.getCableBusAdjacent().contains(link.getKey()), shell);
            state.getAttachmentConnections().forEach((side, distance) ->
                    builder.addConstrainedGlassConnection(side, AEColor.WHITE, distance, shell));
        }
        List<BakedQuad> result = new ArrayList<>(shell.size() + 32);
        for (BakedQuad quad : shell) result.add(tint(quad, color));
        CubeBuilder indicators = new CubeBuilder(original.get(0).getFormat(), result);
        indicators.setTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/quartz_block_side"));
        indicators.setColorRGB(VisualPalette.shade(color, 0.95));
        state.getChannelsOnSide().forEach((side, count) -> {
            if (links.containsKey(side) || state.getAttachmentConnections().containsKey(side))
                addIndicators(indicators, side, Math.min(4, (count + 7) / 8),
                        links.containsKey(side) ? 0 : state.getAttachmentConnections().get(side));
        });
        return result;
    }

    private static BakedQuad tint(BakedQuad quad, int color) {
        int offset = quad.getFormat().getColorOffset();
        int stride = quad.getFormat().getIntegerSize();
        if (offset < 0 || offset % 4 != 0 || offset / 4 >= stride) return quad;
        int[] data = quad.getVertexData().clone();
        for (int i = offset / 4; i < data.length; i += stride) {
            int pixel = data[i];
            int r = (pixel & 255) * (color >> 16 & 255) / 255;
            int g = (pixel >> 8 & 255) * (color >> 8 & 255) / 255;
            int b = (pixel >> 16 & 255) * (color & 255) / 255;
            data[i] = pixel & 0xff000000 | b << 16 | g << 8 | r;
        }
        return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
                quad.shouldApplyDiffuseLighting(), quad.getFormat());
    }

    private static void addIndicators(CubeBuilder builder, EnumFacing side, int count, int distance) {
        if (count == 0 || distance >= 6) return;
        float spacing = (6 - distance) / 5.0f;
        for (int n = 1; n <= count; n++) {
            float position = 6 - n * spacing;
            float center = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 16 - position : position;
            float half = Math.min(.25f, spacing * .35f);
            switch (side.getAxis()) {
                case X:
                    builder.addCube(center-half, 9.98f, 7.1f, center+half, 10.03f, 8.9f);
                    builder.addCube(center-half, 7.1f, 9.98f, center+half, 8.9f, 10.03f);
                    break;
                case Y:
                    builder.addCube(7.1f, center-half, 9.98f, 8.9f, center+half, 10.03f);
                    builder.addCube(9.98f, center-half, 7.1f, 10.03f, center+half, 8.9f);
                    break;
                case Z:
                    builder.addCube(7.1f, 9.98f, center-half, 8.9f, 10.03f, center+half);
                    builder.addCube(9.98f, 7.1f, center-half, 10.03f, 8.9f, center+half);
                    break;
            }
        }
    }
}

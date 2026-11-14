package appeng.client.render.cablebus;

import java.util.List;
import java.util.Map;
import appeng.api.util.AECableType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

/** Код аддона: соседний пакет необходим для доступа к package-private CableBuilder AE2. */
public final class ExpertDenseAttachmentBridge {
    private ExpertDenseAttachmentBridge() { }
    public static void add(CableBuilder builder,CableBusRenderState state,List<BakedQuad> quads) {
        AECableType type=state.getCableType();
        if(type!=AECableType.DENSE_SMART&&type!=AECableType.DENSE_COVERED)return;
        for(Map.Entry<EnumFacing,Integer> side:state.getAttachmentConnections().entrySet()) {
            if(type==AECableType.DENSE_SMART) {
                Integer channels=state.getChannelsOnSide().get(side.getKey());
                builder.addConstrainedSmartConnection(side.getKey(),state.getCableColor(),side.getValue(),Math.min(8,channels==null?0:(channels+3)/4),quads);
            } else builder.addConstrainedCoveredConnection(side.getKey(),state.getCableColor(),side.getValue(),quads);
        }
    }
}

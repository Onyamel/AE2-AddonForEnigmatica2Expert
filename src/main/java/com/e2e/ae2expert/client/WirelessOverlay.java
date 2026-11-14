package com.e2e.ae2expert.client;

import com.e2e.ae2expert.wireless.WirelessTile;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

final class WirelessOverlay {
    private static World world;
    private static final Map<BlockPos, Map<UUID, BlockPos>> visible = new HashMap<>();

    private static void checkWorld() {
        World current = Minecraft.getMinecraft().world;
        if (current != world) {
            visible.clear();
            world = current;
        }
    }

    static boolean shown(BlockPos hub, UUID id) {
        checkWorld();
        return visible.containsKey(hub) && visible.get(hub).containsKey(id);
    }

    static void toggle(BlockPos hub, WirelessTile.Link link) {
        checkWorld();
        Map<UUID, BlockPos> links = visible.computeIfAbsent(hub, key -> new HashMap<>());
        if (links.remove(link.identity) == null) links.put(link.identity, link.pos);
        if (links.isEmpty()) visible.remove(hub);
    }

    static void reconcile(BlockPos hub, List<WirelessTile.Link> entries) {
        checkWorld();
        Map<UUID, BlockPos> links = visible.get(hub);
        if (links == null) return;
        Set<UUID> ids = new HashSet<>();
        for (WirelessTile.Link entry : entries) ids.add(entry.identity);
        links.keySet().retainAll(ids);
        if (links.isEmpty()) visible.remove(hub);
    }

    @SubscribeEvent public void hover(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) return;
        net.minecraft.tileentity.TileEntity tile = mc.world.getTileEntity(mc.objectMouseOver.getBlockPos());
        if (tile instanceof WirelessTile && ((WirelessTile) tile).isHub())
            event.getLeft().add(I18n.format("ae2expert.radio.count", ((WirelessTile) tile).connectionCount()));
    }

    @SubscribeEvent public void render(RenderWorldLastEvent event) {
        checkWorld();
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.entity.Entity view = mc.getRenderViewEntity();
        if (visible.isEmpty() || view == null) return;
        float t = event.getPartialTicks();
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * t,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * t,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * t);
        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_LINE_BIT);
        try {
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GL11.glLineWidth(3f);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (Map.Entry<BlockPos, Map<UUID, BlockPos>> hub : visible.entrySet()) {
                for (BlockPos relay : hub.getValue().values()) {
                    vertex(buffer, hub.getKey(), camera);
                    vertex(buffer, relay, camera);
                }
            }
            tess.draw();
        } finally {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GL11.glPopAttrib();
            GlStateManager.popMatrix();
        }
    }

    private static void vertex(BufferBuilder buffer, BlockPos pos, Vec3d camera) {
        buffer.pos(pos.getX() + .5 - camera.x, pos.getY() + .65 - camera.y, pos.getZ() + .5 - camera.z)
                .color(55, 223, 221, 125).endVertex();
    }
}

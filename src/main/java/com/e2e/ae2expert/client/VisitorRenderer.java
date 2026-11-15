package com.e2e.ae2expert.client;

import com.e2e.ae2expert.encounter.Visitor;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

final class VisitorRenderer extends RenderLiving<Visitor> {
    private static final ResourceLocation SKIN = new ResourceLocation("ae2expert", "textures/entity/visitor.png");

    VisitorRenderer(RenderManager manager) {
        super(manager, new ModelPlayer(0, false), .35f);
    }

    @Override protected ResourceLocation getEntityTexture(Visitor visitor) { return SKIN; }
}

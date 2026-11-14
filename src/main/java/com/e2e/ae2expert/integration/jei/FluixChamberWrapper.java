package com.e2e.ae2expert.integration.jei;

import com.e2e.ae2expert.machine.FluixChamberRecipe;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public final class FluixChamberWrapper implements IRecipeWrapper {
    private final FluixChamberRecipe recipe;
    public FluixChamberWrapper(FluixChamberRecipe recipe) { this.recipe=recipe; }
    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM,recipe.inputs());
        ingredients.setOutput(VanillaTypes.ITEM,recipe.output());
    }
    @Override public void drawInfo(Minecraft mc,int width,int height,int mouseX,int mouseY) {
        mc.fontRenderer.drawString("+",25,22,0x606060);
        mc.fontRenderer.drawString("+",62,22,0x606060);
        mc.fontRenderer.drawString("->",106,22,0x606060);
        mc.fontRenderer.drawString(I18n.format("ae2expert.jei.energy",FluixChamberRecipe.TOTAL_RF,FluixChamberRecipe.RF_PER_TICK),2,46,0x505050);
        mc.fontRenderer.drawString(I18n.format("ae2expert.jei.time",FluixChamberRecipe.DURATION/20),2,59,0x505050);
    }
}

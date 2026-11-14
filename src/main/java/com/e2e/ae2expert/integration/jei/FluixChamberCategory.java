package com.e2e.ae2expert.integration.jei;

import com.e2e.ae2expert.machine.FluixChamberRecipe;
import com.e2e.ae2expert.machine.MachineRegistry;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public final class FluixChamberCategory implements IRecipeCategory<FluixChamberWrapper> {
    private final IDrawable background,icon,slot;
    public FluixChamberCategory(IGuiHelper helper) {
        background=helper.createBlankDrawable(158,74);
        icon=helper.createDrawableIngredient(new ItemStack(MachineRegistry.FLUIX_CHAMBER));
        slot=helper.getSlotDrawable();
    }
    @Override public String getUid() { return FluixChamberRecipe.UID; }
    @Override public String getTitle() { return I18n.format("tile.ae2expert.fluix_chamber.name"); }
    @Override public String getModName() { return "AE2 Expert Addon"; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void drawExtras(Minecraft mc) {
        for(int x:new int[]{0,37,74,132})slot.draw(mc,x,16);
    }
    @Override public void setRecipe(IRecipeLayout layout,FluixChamberWrapper recipe,IIngredients ingredients) {
        IGuiItemStackGroup stacks=layout.getItemStacks();
        stacks.init(0,true,0,16);stacks.init(1,true,37,16);stacks.init(2,true,74,16);stacks.init(3,false,132,16);
        stacks.set(ingredients);
    }
}

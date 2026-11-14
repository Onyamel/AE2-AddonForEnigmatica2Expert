package com.e2e.ae2expert.integration.jei;

import java.util.Collections;
import com.e2e.ae2expert.client.RecipeViewer;
import com.e2e.ae2expert.machine.FluixChamberRecipe;
import com.e2e.ae2expert.machine.MachineRegistry;
import mezz.jei.api.*;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

/** Стандартная категория JEI 4; HEI использует тот же совместимый API и modid jei. */
@JEIPlugin
public final class ExpertJeiPlugin implements IModPlugin {
    public static IJeiRuntime runtime;
    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new FluixChamberCategory(registry.getJeiHelpers().getGuiHelper()));
    }
    @Override public void register(IModRegistry registry) {
        registry.handleRecipes(FluixChamberRecipe.class,recipe->new FluixChamberWrapper(recipe),FluixChamberRecipe.UID);
        registry.addRecipes(Collections.singletonList(FluixChamberRecipe.INSTANCE),FluixChamberRecipe.UID);
        registry.addRecipeCatalyst(new ItemStack(MachineRegistry.FLUIX_CHAMBER),FluixChamberRecipe.UID);
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        ExpertJeiPlugin.runtime=runtime;
        RecipeViewer.register(()->runtime.getRecipesGui().showCategories(Collections.singletonList(FluixChamberRecipe.UID)));
        System.out.println("[AE2Expert] Категория рецепта камеры кристаллов JEI/HEI готова");
    }
}

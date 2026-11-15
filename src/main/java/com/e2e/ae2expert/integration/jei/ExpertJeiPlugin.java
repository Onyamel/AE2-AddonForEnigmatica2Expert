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
        if (com.e2e.ae2expert.extended.TableRegistry.enabled()) {
            mezz.jei.api.recipe.transfer.IRecipeTransferRegistry transfer = registry.getRecipeTransferRegistry();
            mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper errors =
                    registry.getJeiHelpers().recipeTransferHandlerHelper();
            transfer.addRecipeTransferHandler(new TableTransferHandler(3, errors), "minecraft.crafting");
            for (int width : new int[]{3, 5, 7, 9}) {
                transfer.addRecipeTransferHandler(new TableTransferHandler(width, errors),
                        "extendedcrafting:table_crafting_" + width + "x" + width);
            }
        }
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

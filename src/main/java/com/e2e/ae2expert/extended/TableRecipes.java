package com.e2e.ae2expert.extended;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

final class TableRecipes {
    private TableRecipes() { }

    static IRecipe find(InventoryCrafting grid, World world) {
        if (!Loader.isModLoaded("extendedcrafting")) return null;
        for (Object candidate : Integration.recipes()) {
            if (candidate instanceof IRecipe && ((IRecipe) candidate).matches(grid, world))
                return (IRecipe) candidate;
        }
        if (grid.getWidth() == 3 && Integration.useVanillaRecipes()) {
            for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
                if (recipe.matches(grid, world)) return recipe;
            }
        }
        return null;
    }

    // Extended Crafting 1.5.6 keeps its table recipes outside the Forge registry.
    private static final class Integration {
        private static final Object MANAGER;
        private static final Method RECIPES;
        private static final java.lang.reflect.Field VANILLA;

        static {
            try {
                Class<?> manager = Class.forName("com.blakebr0.extendedcrafting.crafting.table.TableRecipeManager");
                MANAGER = manager.getMethod("getInstance").invoke(null);
                RECIPES = manager.getMethod("getRecipes");
                VANILLA = Class.forName("com.blakebr0.extendedcrafting.config.ModConfig")
                        .getField("confTableUseRecipes");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unsupported Extended Crafting table API", e);
            }
        }

        static List<?> recipes() {
            try {
                return (List<?>) RECIPES.invoke(MANAGER);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read Extended Crafting table recipes", e);
            }
        }

        static boolean useVanillaRecipes() {
            try {
                return VANILLA.getBoolean(null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read Extended Crafting configuration", e);
            }
        }
    }
}

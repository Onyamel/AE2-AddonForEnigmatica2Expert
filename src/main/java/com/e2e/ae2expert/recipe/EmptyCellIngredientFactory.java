package com.e2e.ae2expert.recipe;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;

/** Улучшение разрешено только для пустой ячейки без установленных карт. */
public final class EmptyCellIngredientFactory implements IIngredientFactory {
    @Override public Ingredient parse(JsonContext context, JsonObject json) {
        final ItemStack template = CraftingHelper.getItemStack(json, context);
        return new Ingredient(template) {
            @Override public boolean apply(ItemStack stack) {
                if (!super.apply(stack)) return false;
                if (!stack.hasTagCompound()) return true;
                if (stack.getTagCompound().getTagList("expertItems", 10).tagCount() > 0) return false;
                if (stack.getTagCompound().getInteger("ic") > 0) return false;
                // Карты AE2 лежат в составном теге upgrades; не допускаем их потери.
                net.minecraft.nbt.NBTTagCompound upgrades = stack.getTagCompound().getCompoundTag("upgrades");
                net.minecraft.nbt.NBTTagList cards = upgrades.getTagList("Items", 10);
                for (int i = 0; i < cards.tagCount(); i++) {
                    if (!new ItemStack(cards.getCompoundTagAt(i)).isEmpty()) return false;
                }
                return true;
            }
            @Override public boolean isSimple() { return false; }
        };
    }
}

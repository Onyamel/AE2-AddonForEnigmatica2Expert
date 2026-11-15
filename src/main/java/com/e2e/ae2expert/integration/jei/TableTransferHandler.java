package com.e2e.ae2expert.integration.jei;

import com.e2e.ae2expert.extended.ContainerTableTerminal;
import com.e2e.ae2expert.extended.TableRecipeTransfer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

final class TableTransferHandler implements IRecipeTransferHandler<ContainerTableTerminal> {
    private final int size;
    private final IRecipeTransferHandlerHelper errors;

    TableTransferHandler(int size, IRecipeTransferHandlerHelper errors) {
        this.size = size;
        this.errors = errors;
    }

    @Override public Class<ContainerTableTerminal> getContainerClass() { return ContainerTableTerminal.class; }

    @Override public IRecipeTransferError transferRecipe(ContainerTableTerminal container, IRecipeLayout layout,
            EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (size > container.width)
            return errors.createUserErrorWithTooltip(I18n.format("gui.ae2expert.table_tier_required", size, size));
        Map<Integer, ? extends IGuiIngredient<ItemStack>> slots = layout.getItemStacks().getGuiIngredients();
        List<ItemStack> inputs = new ArrayList<>(Collections.nCopies(size * size, ItemStack.EMPTY));
        ItemStack result = ItemStack.EMPTY;
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : slots.entrySet()) {
            IGuiIngredient<ItemStack> slot = entry.getValue();
            ItemStack displayed = slot.getDisplayedIngredient();
            if ((displayed == null || displayed.isEmpty()) && !slot.getAllIngredients().isEmpty())
                displayed = slot.getAllIngredients().get(0);
            if (displayed == null || displayed.isEmpty()) continue;
            if (slot.isInput()) {
                int index = entry.getKey() - 1;
                if (index < 0 || index >= inputs.size()) return errors.createInternalError();
                ItemStack ingredient = displayed.copy();
                ingredient.setCount(1);
                inputs.set(index, ingredient);
            } else if (result.isEmpty()) {
                result = displayed.copy();
            }
        }
        if (result.isEmpty() || inputs.stream().allMatch(ItemStack::isEmpty)) return errors.createInternalError();
        if (doTransfer) TableRecipeTransfer.send(container.windowId, size, inputs, result);
        return null;
    }
}

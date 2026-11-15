package com.e2e.ae2expert.extended;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

final class TablePatternDetails implements ICraftingPatternDetails, PatternMatrix.Sized {
    private final ItemStack pattern;
    private final int width;
    private final IAEItemStack[] inputs;
    private final IAEItemStack[] condensedInputs;
    private final IAEItemStack output;
    private int priority;

    TablePatternDetails(ItemStack pattern, InventoryCrafting grid, ItemStack result) {
        this.pattern = pattern.copy();
        width = grid.getWidth();
        IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        inputs = new IAEItemStack[grid.getSizeInventory()];
        List<IAEItemStack> condensed = new ArrayList<>();
        for (int slot = 0; slot < inputs.length; slot++) {
            ItemStack ingredient = grid.getStackInSlot(slot);
            if (ingredient.isEmpty()) continue;
            IAEItemStack entry = channel.createStack(ingredient);
            inputs[slot] = entry;
            IAEItemStack existing = null;
            for (IAEItemStack candidate : condensed) {
                if (candidate.equals(entry)) {
                    existing = candidate;
                    break;
                }
            }
            if (existing == null) condensed.add(entry.copy());
            else existing.incStackSize(entry.getStackSize());
        }
        condensedInputs = condensed.toArray(new IAEItemStack[0]);
        output = channel.createStack(result);
    }

    @Override public int matrixWidth() { return width; }
    @Override public ItemStack getPattern() { return pattern.copy(); }
    @Override public boolean isCraftable() { return true; }
    @Override public boolean canSubstitute() { return false; }
    @Override public int getPriority() { return priority; }
    @Override public void setPriority(int value) { priority = value; }
    @Override public IAEItemStack[] getInputs() { return copy(inputs); }
    @Override public IAEItemStack[] getCondensedInputs() { return copy(condensedInputs); }
    @Override public IAEItemStack[] getOutputs() { return new IAEItemStack[]{output.copy()}; }
    @Override public IAEItemStack[] getCondensedOutputs() { return getOutputs(); }

    @Override
    public boolean isValidItemForSlot(int slot, ItemStack stack, World world) {
        if (slot < 0 || slot >= inputs.length) return false;
        IAEItemStack expected = inputs[slot];
        return expected == null ? stack.isEmpty() : expected.equals(
                AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(stack));
    }

    @Override
    public ItemStack getOutput(InventoryCrafting grid, World world) {
        if (grid.getWidth() != width || grid.getHeight() != width) return ItemStack.EMPTY;
        IRecipe recipe = TableRecipes.find(grid, world);
        if (recipe == null) return ItemStack.EMPTY;
        ItemStack result = recipe.getCraftingResult(grid);
        return ItemStack.areItemStacksEqual(output.createItemStack(), result) ? result : ItemStack.EMPTY;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TablePatternDetails
                && ItemStack.areItemStacksEqual(pattern, ((TablePatternDetails) obj).pattern);
    }

    @Override public int hashCode() { return 31 * pattern.getItem().hashCode() + width; }

    private static IAEItemStack[] copy(IAEItemStack[] stacks) {
        IAEItemStack[] result = new IAEItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) result[i] = stacks[i] == null ? null : stacks[i].copy();
        return result;
    }
}

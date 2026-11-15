package com.e2e.ae2expert.extended;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

public final class ItemTablePattern extends Item implements ICraftingPatternItem {
    public ItemTablePattern() {
        setRegistryName("ae2expert", "table_pattern");
        setTranslationKey("ae2expert.table_pattern");
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void addInformation(ItemStack stack, World world, java.util.List<String> tooltip,
            net.minecraft.client.util.ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) return;
        boolean processing = tag.getBoolean("processing");
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        NBTTagList list = tag.getTagList(processing ? "in" : "inputs", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.tagCount(), processing ? 9 : 81); i++) {
            ItemStack item = new ItemStack(list.getCompoundTagAt(i));
            if (item.isEmpty()) continue;
            ItemStack existing = inputs.stream().filter(other -> ItemStack.areItemsEqual(other, item)
                    && ItemStack.areItemStackTagsEqual(other, item)).findFirst().orElse(ItemStack.EMPTY);
            if (existing.isEmpty()) inputs.add(item);
            else existing.grow(item.getCount());
        }
        if (processing) {
            NBTTagList outputs = tag.getTagList("out", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < Math.min(3, outputs.tagCount()); i++)
                describe(new ItemStack(outputs.getCompoundTagAt(i)), "gui.ae2expert.table_pattern_output", tooltip);
        } else describe(new ItemStack(tag.getCompoundTag("output")), "gui.ae2expert.table_pattern_output", tooltip);
        for (ItemStack input : inputs) describe(input, "gui.ae2expert.table_pattern_input", tooltip);
    }

    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    private static void describe(ItemStack item, String key, java.util.List<String> tooltip) {
        if (!item.isEmpty()) tooltip.add(net.minecraft.client.resources.I18n.format(key, item.getCount(), item.getDisplayName()));
    }

    public ItemStack encode(InventoryCrafting grid, World world) {
        int width = grid.getWidth();
        if (!validWidth(width) || grid.getHeight() != width || world == null) return ItemStack.EMPTY;
        InventoryCrafting sample = matrix(width);
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack ingredient = grid.getStackInSlot(slot);
            if (!ingredient.isEmpty()) {
                ItemStack single = ingredient.copy();
                single.setCount(1);
                sample.setInventorySlotContents(slot, single);
            }
        }
        IRecipe recipe = TableRecipes.find(sample, world);
        if (recipe == null) return ItemStack.EMPTY;
        ItemStack result = recipe.getCraftingResult(sample);
        if (result.isEmpty()) return ItemStack.EMPTY;
        NBTTagList slots = new NBTTagList();
        for (int slot = 0; slot < sample.getSizeInventory(); slot++) {
            ItemStack stack = sample.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            NBTTagCompound entry = stack.writeToNBT(new NBTTagCompound());
            entry.setInteger("slot", slot);
            slots.appendTag(entry);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("width", width);
        tag.setTag("inputs", slots);
        tag.setTag("output", result.writeToNBT(new NBTTagCompound()));
        ItemStack pattern = new ItemStack(this);
        pattern.setTagCompound(tag);
        return pattern;
    }

    ItemStack encodeProcessing(InventoryCrafting grid, net.minecraft.inventory.IInventory outputs) {
        NBTTagList inputs = new NBTTagList();
        NBTTagList results = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = grid.getStackInSlot(i / 3 * grid.getWidth() + i % 3);
            inputs.appendTag(stack.isEmpty() ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));
        }
        for (int i = 0; i < outputs.getSizeInventory(); i++) {
            ItemStack stack = outputs.getStackInSlot(i);
            if (!stack.isEmpty()) results.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        boolean hasInput = false;
        for (int i = 0; i < inputs.tagCount(); i++)
            hasInput |= !new ItemStack(inputs.getCompoundTagAt(i)).isEmpty();
        if (!hasInput || results.tagCount() == 0) return ItemStack.EMPTY;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("processing", true);
        tag.setTag("in", inputs);
        tag.setTag("out", results);
        ItemStack pattern = new ItemStack(this);
        pattern.setTagCompound(tag);
        return pattern;
    }

    @Override
    public ICraftingPatternDetails getPatternForItem(ItemStack stack, World world) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || world == null) return null;
        if (tag.getBoolean("processing")) {
            if (tag.getBoolean("crafting") || tag.getTagList("in", 10).tagCount() != 9
                    || tag.getTagList("out", 10).tagCount() < 1 || tag.getTagList("out", 10).tagCount() > 3) return null;
            try {
                return new appeng.helpers.PatternHelper(stack, world);
            } catch (RuntimeException invalid) {
                return null;
            }
        }
        int width = tag.getInteger("width");
        if (!validWidth(width)) return null;
        InventoryCrafting grid = matrix(width);
        NBTTagList slots = tag.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
        if (slots.tagCount() == 0 || slots.tagCount() > grid.getSizeInventory()) return null;
        for (int i = 0; i < slots.tagCount(); i++) {
            NBTTagCompound entry = slots.getCompoundTagAt(i);
            if (!entry.hasKey("slot", Constants.NBT.TAG_INT)) return null;
            int slot = entry.getInteger("slot");
            if (slot < 0 || slot >= grid.getSizeInventory() || !grid.getStackInSlot(slot).isEmpty()) return null;
            ItemStack input = new ItemStack(entry);
            if (input.isEmpty() || input.getCount() != 1) return null;
            grid.setInventorySlotContents(slot, input);
        }
        ItemStack output = new ItemStack(tag.getCompoundTag("output"));
        IRecipe recipe = TableRecipes.find(grid, world);
        if (output.isEmpty() || recipe == null
                || !ItemStack.areItemStacksEqual(output, recipe.getCraftingResult(grid))) return null;
        ItemStack definition = stack.copy();
        definition.setCount(1);
        return new TablePatternDetails(definition, grid, output);
    }

    private static boolean validWidth(int width) {
        return width == 3 || width == 5 || width == 7 || width == 9;
    }

    private static InventoryCrafting matrix(int width) {
        return new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return false; }
        }, width, width);
    }
}

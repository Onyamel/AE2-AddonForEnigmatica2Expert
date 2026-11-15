package com.e2e.ae2expert.extended;

import appeng.api.AEApi;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public final class ContainerTableTerminal extends Container {
    private final InventoryCrafting grid;
    private final net.minecraft.inventory.InventoryBasic outputs =
            new net.minecraft.inventory.InventoryBasic("outputs", false, 3);
    private final net.minecraft.inventory.InventoryBasic previews =
            new net.minecraft.inventory.InventoryBasic("preview", false, 2);
    private final EntityPlayer player;
    private boolean previewDirty = true;
    private boolean processing;

    public boolean isProcessing() { return processing; }
    public void toggleClientMode() { setProcessing(!processing); }

    public int inventoryTop() { return processing ? 136 : inventoryTop; }
    public int controlsLeft() { return processing ? 132 : controlsLeft; }

    private void setProcessing(boolean value) {
        processing = value;
        previewDirty = true;
        int firstPreview = width * width + outputs.getSizeInventory();
        for (int i = 0; i < 2; i++) getSlot(firstPreview + i).xPos = controlsLeft() + 11 + i * 35;
        int firstPlayerSlot = width * width + outputs.getSizeInventory() + previews.getSizeInventory();
        for (int i = 0; i < 36; i++) {
            getSlot(firstPlayerSlot + i).yPos = inventoryTop() + (i < 27 ? i / 9 * 18 : 58);
        }
    }

    public boolean canEditQuantity(Slot slot) {
        return processing && slot.isEnabled() && (slot.inventory == grid || slot.inventory == outputs) && slot.getHasStack();
    }
    private final BlockPos pos;
    private final BlockTableTerminal terminal;
    public final int width;
    private final int inventoryTop;
    private final int controlsLeft;
    private int recipeWidth;

    public ContainerTableTerminal(EntityPlayer player, BlockPos pos, BlockTableTerminal terminal) {
        this.player = player;
        this.pos = pos;
        this.terminal = terminal;
        width = terminal.width;
        recipeWidth = width;
        inventoryTop = Math.max(136, 26 + 18 * width);
        controlsLeft = Math.max(132, 16 + 18 * width);
        grid = new InventoryCrafting(this, width, width);
        outputs.addInventoryChangeListener(inventory -> previewDirty = true);
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            addSlotToContainer(new Slot(grid, i, 9 + 18 * (i % width), 18 + 18 * (i / width)) {
                @Override public boolean isEnabled() {
                    return !processing || getSlotIndex() / width < 3 && getSlotIndex() % width < 3;
                }
                @Override public boolean isItemValid(ItemStack stack) { return false; }
                @Override public boolean canTakeStack(EntityPlayer owner) { return false; }
            });
        }
        for (int i = 0; i < 3; i++) {
            addSlotToContainer(new Slot(outputs, i, 69 + i * 18, 36) {
                @Override public boolean isEnabled() { return processing; }
                @Override public boolean isItemValid(ItemStack stack) { return false; }
                @Override public boolean canTakeStack(EntityPlayer owner) { return false; }
            });
        }
        for (int i = 0; i < 2; i++) {
            addSlotToContainer(new Slot(previews, i, controlsLeft + 11 + i * 35, 113) {
                @Override public boolean isItemValid(ItemStack stack) { return false; }
                @Override public boolean canTakeStack(EntityPlayer owner) { return false; }
            });
        }
        for (int i = 0; i < 36; i++) {
            int y = i < 27 ? inventoryTop + i / 9 * 18 : inventoryTop + 58;
            addSlotToContainer(new Slot(player.inventory, i < 27 ? i + 9 : i - 27, 9 + i % 9 * 18, y));
        }
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return player.world.isBlockLoaded(pos) && player.world.getBlockState(pos).getBlock() == terminal
                && player.getDistanceSqToCenter(pos) <= 64;
    }

    @Override public ItemStack slotClick(int id, int mouse, ClickType type, EntityPlayer player) {
        int firstPreview = grid.getSizeInventory() + outputs.getSizeInventory();
        if (id >= firstPreview && id < firstPreview + previews.getSizeInventory()) return ItemStack.EMPTY;
        if (id >= 0 && id < firstPreview) {
            if (type == ClickType.PICKUP && canInteractWith(player) && getSlot(id).isEnabled()) {
                ItemStack selection = mouse == 0 ? player.inventory.getItemStack().copy() : ItemStack.EMPTY;
                if (!selection.isEmpty()) selection.setCount(1);
                getSlot(id).putStack(selection);
                previewDirty = true;
                if (!processing && !selection.isEmpty() && (id / width >= recipeWidth || id % width >= recipeWidth)) recipeWidth = width;
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(id, mouse, type, player);
    }

    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != grid && slot.inventory != outputs && slot.inventory != previews && super.canMergeSlot(stack, slot);
    }

    @Override public boolean canDragIntoSlot(Slot slot) {
        return slot.inventory != grid && slot.inventory != outputs && slot.inventory != previews && super.canDragIntoSlot(slot);
    }

    @Override public boolean enchantItem(EntityPlayer player, int action) {
        if (!(player instanceof EntityPlayerMP) || !canInteractWith(player)) return false;
        if (action == 3) {
            setProcessing(!processing);
            return true;
        }
        if (action == 1) {
            outputs.clear();
            grid.clear();
            recipeWidth = width;
            detectAndSendChanges();
            return true;
        }
        if (action == 2 && !processing) {
            ItemStack held = player.getHeldItemMainhand();
            if (held.getItem() != TableRegistry.PATTERN) return false;
            appeng.api.networking.crafting.ICraftingPatternDetails details =
                    TableRegistry.PATTERN.getPatternForItem(held, player.world);
            if (!(details instanceof TablePatternDetails)) return false;
            int originalWidth = ((TablePatternDetails) details).matrixWidth();
            if (originalWidth > width) return false;
            grid.clear();
            recipeWidth = originalWidth;
            appeng.api.storage.data.IAEItemStack[] inputs = details.getInputs();
            for (int i = 0; i < inputs.length; i++) {
                if (inputs[i] != null) grid.setInventorySlotContents(
                        i / originalWidth * width + i % originalWidth, inputs[i].createItemStack());
            }
            previews.setInventorySlotContents(1, held.copy());
            detectAndSendChanges();
            return true;
        }
        if (action != 0) return false;
        ItemStack blank = AEApi.instance().definitions().materials().blankPattern().maybeStack(1).orElse(ItemStack.EMPTY);
        if (blank.isEmpty()) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!blank.isItemEqual(stack) || stack.hasTagCompound()) continue;
            ItemStack encoded = processing ? TableRegistry.PATTERN.encodeProcessing(grid, outputs)
                    : TableRegistry.PATTERN.encode(recipeGrid(), player.world);
            if (encoded.isEmpty()) return false;
            previews.setInventorySlotContents(1, encoded.copy());
            stack.shrink(1);
            if (!player.inventory.addItemStackToInventory(encoded)) player.dropItem(encoded, false);
            detectAndSendChanges();
            return true;
        }
        return false;
    }

    private InventoryCrafting recipeGrid() {
        InventoryCrafting result = new InventoryCrafting(new appeng.container.ContainerNull(), recipeWidth, recipeWidth);
        for (int i = 0; i < result.getSizeInventory(); i++) {
            ItemStack sample = grid.getStackInSlot(i / recipeWidth * width + i % recipeWidth).copy();
            if (!sample.isEmpty()) sample.setCount(1);
            result.setInventorySlotContents(i, sample);
        }
        return result;
    }

    @Override public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {
        previewDirty = true;
    }

    @Override public void detectAndSendChanges() {
        if (!player.world.isRemote && previewDirty) {
            previewDirty = false;
            ItemStack result = ItemStack.EMPTY;
            if (!processing && !grid.isEmpty()) {
                InventoryCrafting matrix = recipeGrid();
                net.minecraft.item.crafting.IRecipe recipe = TableRecipes.find(matrix, player.world);
                if (recipe != null) result = recipe.getCraftingResult(matrix);
            }
            if (processing) result = outputs.getStackInSlot(0).copy();
            previews.setInventorySlotContents(0, result);
        }
        super.detectAndSendChanges();
    }

    boolean transferRecipe(int size, java.util.List<ItemStack> ingredients, ItemStack expected) {
        if (size > width || size != 3 && size != 5 && size != 7 && size != 9
                || ingredients.size() != size * size || expected.isEmpty()) return false;
        InventoryCrafting matrix = new InventoryCrafting(new appeng.container.ContainerNull(), size, size);
        for (int i = 0; i < ingredients.size(); i++) matrix.setInventorySlotContents(i, ingredients.get(i));
        net.minecraft.item.crafting.IRecipe recipe = TableRecipes.find(matrix, player.world);
        if (recipe == null) return false;
        ItemStack result = recipe.getCraftingResult(matrix);
        if (!ItemStack.areItemsEqual(result, expected)) return false;
        if (processing) {
            java.util.List<ItemStack> condensed = new java.util.ArrayList<>();
            for (ItemStack input : ingredients) {
                if (input.isEmpty()) continue;
                ItemStack existing = condensed.stream().filter(s -> ItemStack.areItemsEqual(s, input)
                        && ItemStack.areItemStackTagsEqual(s, input)).findFirst().orElse(ItemStack.EMPTY);
                if (existing.isEmpty()) condensed.add(input.copy());
                else if (existing.getCount() < 64) existing.grow(1);
                else return false;
            }
            if (condensed.size() > 9) return false;
            grid.clear();
            outputs.clear();
            for (int i = 0; i < condensed.size(); i++) grid.setInventorySlotContents(i / 3 * width + i % 3, condensed.get(i));
            outputs.setInventorySlotContents(0, result);
        } else {
            grid.clear();
            recipeWidth = size;
            for (int i = 0; i < ingredients.size(); i++)
                grid.setInventorySlotContents(i / size * width + i % size, ingredients.get(i));
        }
        previewDirty = true;
        detectAndSendChanges();
        return true;
    }

    public static ContainerTableTerminal open(EntityPlayer player, BlockPos pos) {
        Block block = player.world.getBlockState(pos).getBlock();
        return block instanceof BlockTableTerminal ? new ContainerTableTerminal(player, pos, (BlockTableTerminal) block) : null;
    }
}

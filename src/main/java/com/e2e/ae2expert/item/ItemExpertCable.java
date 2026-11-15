package com.e2e.ae2expert.item;

import net.minecraft.item.ItemStack;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;
import appeng.items.AEBaseItem;

import com.e2e.ae2expert.api.ICableColorItem;
import com.e2e.ae2expert.cable.CableTier;
import com.e2e.ae2expert.part.PartExpertCable;

/**
 * Item form of the addon's channel cable.
 *
 * <p>The item is intentionally not an AE2 {@code ItemPart}: the coremod patch on {@code PartCable}
 * resolves the colour through {@link ICableColorItem} instead of casting.</p>
 */
public class ItemExpertCable extends ExpertPartItem implements ICableColorItem {

    private final CableTier tier;

    public ItemExpertCable(CableTier tier) {
        this.tier = tier;
        this.setRegistryName("ae2expert", "cable_" + tier.id);
        this.setTranslationKey("ae2expert.cable_" + tier.id);
        this.setMaxStackSize(64);
        setHasSubtypes(true);
    }

    public CableTier getTier() {
        return this.tier;
    }

    public int getChannelCapacity() {
        return this.tier.capacity;
    }

    @Override
    public AEColor getCableColor() {
        return this.tier.color;
    }

    @Override
    public void getCheckedSubItems(net.minecraft.creativetab.CreativeTabs tab, net.minecraft.util.NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (int meta = 0; meta <= 16; meta++) items.add(new ItemStack(this, 1, meta));
        }
    }

    public int paintColor(int metadata, int fallback) {
        AEColor[] colors = AEColor.values();
        if (metadata < 1 || metadata > 16) return fallback;
        int color = colors[metadata - 1].mediumVariant;
        int gray = 128;
        return ((((color >> 16 & 255) * 2 + gray) / 3) << 16)
                | ((((color >> 8 & 255) * 2 + gray) / 3) << 8)
                | (((color & 255) * 2 + gray) / 3);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        int meta = stack.getMetadata();
        String name = super.getItemStackDisplayName(stack);
        if (meta < 1 || meta > 16) return name;
        return name + " (" + net.minecraft.util.text.translation.I18n.translateToLocal(
                AEColor.values()[meta - 1].unlocalizedName) + ")";
    }

    @Override
    public IPart createPartFromItemStack(ItemStack is) {
        return new PartExpertCable(is);
    }
}

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
    public IPart createPartFromItemStack(ItemStack is) {
        return new PartExpertCable(is);
    }
}

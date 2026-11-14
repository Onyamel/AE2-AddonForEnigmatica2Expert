package com.e2e.ae2expert.item;

import java.util.List;
import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import appeng.me.storage.BasicCellInventoryHandler;
import com.e2e.ae2expert.storage.LongCellInventory;
import com.e2e.ae2expert.storage.CellTier;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

/** Собственный обработчик нужен для long; фильтры и улучшения обслуживает AE2. */
public abstract class ExpertStorageCell<T extends IAEStack<T>> extends AEBaseItem implements ICellWorkbenchItem, ICellHandler {
    protected final CellTier tier;
    private final long bytes;
    private final int types, typeBytes;
    private final double drain;
    protected ExpertStorageCell(String name, CellTier tier) {
        this(name, tier, tier.getBytes(), 63, tier.getBytesPerType(), tier.drain);
    }
    protected ExpertStorageCell(String name, CellTier tier, long bytes, int types, int typeBytes, double drain) {
        this.tier = tier;
        this.bytes = bytes;
        this.types = types;
        this.typeBytes = typeBytes;
        this.drain = drain;
        setRegistryName("ae2expert", name);
        setTranslationKey("ae2expert." + name);
        setMaxStackSize(1);
    }
    public CellTier getTier() { return tier; }
    public abstract IStorageChannel<T> getChannel();
    @Override public boolean isCell(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == this; }
    @Override @SuppressWarnings("unchecked")
    public <S extends IAEStack<S>> ICellInventoryHandler<S> getCellInventory(ItemStack stack, ISaveProvider provider, IStorageChannel<S> channel) {
        if (!isCell(stack) || channel != getChannel()) return null;
        return new BasicCellInventoryHandler<>(new LongCellInventory<>(stack, provider, channel, bytes, types, typeBytes, drain), channel);
    }
    @Override public boolean isEditable(ItemStack stack) { return true; }
    @Override public IItemHandler getConfigInventory(ItemStack stack) { return new CellConfig(stack); }
    @Override public IItemHandler getUpgradesInventory(ItemStack stack) { return new CellUpgrades(stack, 2); }
    @Override public FuzzyMode getFuzzyMode(ItemStack stack) {
        try { return FuzzyMode.valueOf(stack.hasTagCompound() ? stack.getTagCompound().getString("FuzzyMode") : ""); }
        catch (IllegalArgumentException missing) { return FuzzyMode.IGNORE_ALL; }
    }
    @Override public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("FuzzyMode", mode.name());
    }
    @Override public void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        lines.add(I18n.translateToLocalFormatted("ae2expert.cell.tooltip.types", types));
        lines.add(I18n.translateToLocalFormatted("ae2expert.cell.tooltip.bytes", CellTier.formatUnits(bytes)));
        lines.add(I18n.translateToLocal("ae2expert.cell.tooltip.empty_upgrade"));
        AEApi.instance().client().addCellInformation(getCellInventory(stack, null, getChannel()), lines);
    }
}

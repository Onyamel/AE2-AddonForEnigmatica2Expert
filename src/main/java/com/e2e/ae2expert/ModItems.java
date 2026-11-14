package com.e2e.ae2expert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.registries.IForgeRegistry;

import com.e2e.ae2expert.bus.BusTier;
import com.e2e.ae2expert.cable.CableTier;
import com.e2e.ae2expert.item.ItemExpertBus;
import com.e2e.ae2expert.item.ItemExpertCable;
import com.e2e.ae2expert.item.ItemExpertCell;
import com.e2e.ae2expert.item.ItemExpertFluidCell;
import com.e2e.ae2expert.item.ItemInfiniteCell;
import com.e2e.ae2expert.storage.CellTier;

/**
 * Central item registry of the addon.
 */
public final class ModItems {

    public static final CreativeTabs TAB = new CreativeTabs("ae2expert") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.itemCells.get(CellTier.K64));
        }
    };

    public static final Map<CellTier, ItemExpertCell> itemCells = new EnumMap<>(CellTier.class);
    public static final Map<CellTier, ItemExpertFluidCell> fluidCells = new EnumMap<>(CellTier.class);
    public static final Map<CableTier, ItemExpertCable> cables = new EnumMap<>(CableTier.class);

    public static ItemInfiniteCell infiniteCell;
    public static final Map<BusTier, ItemExpertBus> importBuses = new EnumMap<>(BusTier.class);
    public static final Map<BusTier, ItemExpertBus> exportBuses = new EnumMap<>(BusTier.class);

    private static final List<Item> ALL = new ArrayList<>();

    private ModItems() {
    }

    public static List<Item> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Creates every item instance. Called from the registry event, once. */
    public static void createItems() {
        if (!ALL.isEmpty()) {
            return;
        }
        for (CellTier tier : CellTier.values()) {
            register(new ItemExpertCell(tier));
            itemCells.put(tier, (ItemExpertCell) ALL.get(ALL.size() - 1));

            register(new ItemExpertFluidCell(tier));
            fluidCells.put(tier, (ItemExpertFluidCell) ALL.get(ALL.size() - 1));
        }

        for (CableTier tier : CableTier.values()) {
            ItemExpertCable cable = new ItemExpertCable(tier);
            register(cable);
            cables.put(tier, cable);
        }

        for (BusTier tier : BusTier.values()) {
            ItemExpertBus imp = new ItemExpertBus(tier, false);
            register(imp);
            importBuses.put(tier, imp);

            ItemExpertBus exp = new ItemExpertBus(tier, true);
            register(exp);
            exportBuses.put(tier, exp);
        }

        if (Loader.isModLoaded("extracells") && Loader.isModLoaded("mekanism")) {
            GasCellIntegration.create();
        }

        infiniteCell = new ItemInfiniteCell();
        register(infiniteCell);
    }

    public static void register(Item item) {
        item.setCreativeTab(TAB);
        ALL.add(item);
    }

    public static void registerAll(IForgeRegistry<Item> registry) {
        for (Item item : ALL) {
            registry.register(item);
        }
    }
}


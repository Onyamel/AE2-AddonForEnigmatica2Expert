package com.e2e.ae2expert.bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import appeng.api.AEApi;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;

/**
 * Part models of the addon's buses.
 *
 * <p>AE2 discovers part models through {@code @PartModels} on its own classes only, so an addon has
 * to hand its resource locations to {@code IPartModels} explicitly. The JSON files are copies of the
 * stock bus models with only the front face texture swapped for a tier coloured one.</p>
 */
public final class BusModels {

    private static final String[] SUFFIXES = { "off", "on", "has_channel" };

    private static final Map<Integer, IPartModel[]> IMPORT = new HashMap<>();
    private static final Map<Integer, IPartModel[]> EXPORT = new HashMap<>();
    private static final List<ResourceLocation> REGISTER = new ArrayList<>();

    private static boolean built;

    private BusModels() {
    }

    private static void build() {
        if (built) {
            return;
        }
        built = true;
        for (int multiplier : new int[] { 4, 8, 16 }) {
            IMPORT.put(multiplier, create("import_bus", multiplier));
            EXPORT.put(multiplier, create("export_bus", multiplier));
        }
    }

    private static IPartModel[] create(String family, int multiplier) {
        ResourceLocation base = new ResourceLocation("ae2expert", "part/" + family + "_base_" + multiplier);
        REGISTER.add(base);

        IPartModel[] models = new IPartModel[3];
        for (int i = 0; i < SUFFIXES.length; i++) {
            ResourceLocation overlay = new ResourceLocation("ae2expert",
                    "part/" + family + "_" + SUFFIXES[i] + "_" + multiplier);
            REGISTER.add(overlay);
            models[i] = new PartModel(base, overlay);
        }
        return models;
    }

    public static IPartModel[] importModels(int multiplier) {
        build();
        return IMPORT.get(multiplier);
    }

    public static IPartModel[] exportModels(int multiplier) {
        build();
        return EXPORT.get(multiplier);
    }

    /** Must be called on the client before models are baked. */
    public static void register() {
        build();
        AEApi.instance().registries().partModels().registerModels(REGISTER);
    }
}

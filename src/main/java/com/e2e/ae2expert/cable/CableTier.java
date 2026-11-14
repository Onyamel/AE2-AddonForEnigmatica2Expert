package com.e2e.ae2expert.cable;

import appeng.api.util.AEColor;

/**
 * Cable tiers added by the addon.
 *
 * <p>AE2 itself only knows 8 (normal) and 32 (dense) channels. The coremod hook lets these cables
 * advertise their real capacity, so a single 256 channel line can replace four dense lines.</p>
 */
public enum CableTier {

    C32("32", 32, AEColor.LIGHT_BLUE, "8b8bff"),
    C64("64", 64, AEColor.CYAN, "4fd6d6"),
    C128("128", 128, AEColor.PURPLE, "b05ce0"),
    C256("256", 256, AEColor.MAGENTA, "ff5ce0");

    /** Suffix used in registry names. */
    public final String id;

    /** Channels this cable can carry. */
    public final int capacity;

    /** AE2 colour, which drives the in world cable rendering. */
    public final AEColor color;

    /** Hex colour used by the texture generator for the inventory icon. */
    public final String hex;

    CableTier(String id, int capacity, AEColor color, String hex) {
        this.id = id;
        this.capacity = capacity;
        this.color = color;
        this.hex = hex;
    }
}

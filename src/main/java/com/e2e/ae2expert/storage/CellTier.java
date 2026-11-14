package com.e2e.ae2expert.storage;

import net.minecraft.util.text.translation.I18n;

/**
 * Tiers of storage cells added by the addon.
 *
 * <p>The {@code kilobytes} field follows the AE2 convention: 1 "k" equals 1024 bytes, and one byte
 * holds {@code unitsPerByte} units of the channel (8 items by default). So a "16k" cell stores
 * 16 * 1024 * 8 = 131072 items, exactly like AE2's own 16k cell.</p>
 *
 * <p>The ladder is a pure doubling progression: 16k, 32k, 64k ... 512M. Every tier doubles the
 * previous one, so the player always knows what the next step gives.</p>
 */
public enum CellTier {

    K16("16k", 16, 0.50F, 0),
    K32("32k", 32, 0.75F, 22),
    K64("64k", 64, 1.00F, 44),
    K128("128k", 128, 1.25F, 66),
    K256("256k", 256, 1.50F, 90),
    K512("512k", 512, 1.75F, 120),
    M1("1M", 1024, 2.00F, 150),
    M2("2M", 2048, 2.25F, 172),
    M4("4M", 4096, 2.50F, 194),
    M8("8M", 8192, 2.75F, 216),
    M16("16M", 16384, 3.00F, 238),
    M32("32M", 32768, 3.25F, 260),
    M64("64M", 65536, 3.50F, 282),
    M128("128M", 131072, 3.75F, 304),
    M256("256M", 262144, 4.00F, 326),
    M512("512M", 524288, 4.25F, 348);

    /** Short human readable capacity label, e.g. {@code "16k"} or {@code "512M"}. */
    public final String label;

    /** Capacity in AE2 "kilobytes" (1k = 1024 bytes). */
    public final int kilobytes;

    /** Idle power drain in AE, matching the AE2 scaling. */
    public final float drain;

    /** Hue used by the texture generator, so every tier gets its own colour. */
    public final int hue;

    CellTier(String label, int kilobytes, float drain, int hue) {
        this.label = label;
        this.kilobytes = kilobytes;
        this.drain = drain;
        this.hue = hue;
    }

    /** Registry path suffix, lower case, e.g. {@code "16k"} or {@code "512m"}. */
    public String getId() {
        return this.label.toLowerCase(java.util.Locale.ROOT);
    }

    /** Total amount of bytes this tier can store. Always fits into an int for this ladder. */
    public int getBytes() {
        return this.kilobytes * 1024;
    }

    /** Bytes reserved for every new item/fluid/gas type, following AE2's ratio (bytes / 128). */
    public int getBytesPerType() {
        return Math.max(8, this.getBytes() / 128);
    }

    /** Units (items, mB, gas) this tier can hold, for tooltips and documentation. */
    public long getUnits(int unitsPerByte) {
        return (long) this.getBytes() * (long) unitsPerByte;
    }

    /** Localised, human readable capacity such as "16k (131 072 предметов)". */
    @SuppressWarnings("deprecation")
    public String getDescription(int unitsPerByte) {
        return I18n.translateToLocalFormatted("ae2expert.tier.desc", this.label, formatUnits(this.getUnits(unitsPerByte)));
    }

    /** Formats a big number with thin spaces as thousands separators: 131072 -> "131 072". */
    public static String formatUnits(long value) {
        String raw = Long.toString(value);
        StringBuilder out = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                out.append(' ');
            }
            out.append(raw.charAt(i));
        }
        return out.toString();
    }
}

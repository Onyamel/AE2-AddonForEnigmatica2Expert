package com.e2e.ae2expert.assembler;

/**
 * Speed tiers of the addon's molecular assemblers.
 *
 * <p>The multiplier is applied by running AE2's crafting tick several times per server tick, so a
 * faster assembler also draws proportionally more AE power - no free lunch.</p>
 */
public enum AssemblerTier {

    X2("2", 2),
    X4("4", 4),
    X8("8", 8),
    X16("16", 16),
    X32("32", 32);

    /** Suffix used in registry names. */
    public final String id;

    /** How many AE2 crafting ticks are simulated per server tick. */
    public final int speed;

    AssemblerTier(String id, int speed) {
        this.id = id;
        this.speed = speed;
    }
}

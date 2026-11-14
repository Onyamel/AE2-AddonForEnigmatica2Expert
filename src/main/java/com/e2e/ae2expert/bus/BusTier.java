package com.e2e.ae2expert.bus;

/**
 * Speed tiers of the addon's import/export buses.
 *
 * <p>A stock AE2 bus moves one item per operation; these multiply that number, which is exactly the
 * knob AE2 itself uses for speed cards ({@code PartSharedItemBus.calculateItemsToSend()}).</p>
 */
public enum BusTier {

    X4("4", 4),
    X8("8", 8),
    X16("16", 16);

    /** Suffix used in registry names. */
    public final String id;

    /** Items moved per operation, relative to a stock bus. */
    public final int multiplier;

    BusTier(String id, int multiplier) {
        this.id = id;
        this.multiplier = multiplier;
    }
}

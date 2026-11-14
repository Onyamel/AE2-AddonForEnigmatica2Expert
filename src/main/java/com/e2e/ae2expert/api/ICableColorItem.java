package com.e2e.ae2expert.api;

import appeng.api.util.AEColor;

/**
 * Implemented by cable items so the coremod hook can resolve the cable colour without the item
 * being an AE2 {@code ItemPart}.
 */
public interface ICableColorItem {

    /** Colour used for this cable. Must not be null. */
    AEColor getCableColor();
}

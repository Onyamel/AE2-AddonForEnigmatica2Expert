package com.e2e.ae2expert.assembler;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.tile.crafting.TileMolecularAssembler;

/**
 * Molecular assembler with configurable speed.
 *
 * <p>AE2's assembler accumulates 100 progress points per craft and its {@code progress} field is
 * private, so the only clean way to speed it up is to run the whole crafting tick several times per
 * server tick. Power is consumed by every simulated tick, which keeps the progression honest.</p>
 *
 * <p>Each tier gets its own class so that breaking a block always drops the matching item - AE2 maps
 * tile entity classes to items one to one.</p>
 */
public abstract class TileExpertAssembler extends TileMolecularAssembler {

    /** How many crafting ticks are simulated per server tick. */
    protected abstract int getSpeedMultiplier();

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        TickRateModulation result = TickRateModulation.IDLE;
        int iterations = Math.max(1, this.getSpeedMultiplier());
        for (int i = 0; i < iterations; i++) {
            result = super.tickingRequest(node, ticksSinceLastCall);
            if (result == TickRateModulation.SLEEP) {
                return result;
            }
        }
        return result;
    }

    /** x2 speed. */
    public static class X2 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 2;
        }
    }

    /** x4 speed. */
    public static class X4 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 4;
        }
    }

    /** x8 speed. */
    public static class X8 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 8;
        }
    }

    /** x16 speed. */
    public static class X16 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 16;
        }
    }

    /** x32 speed. */
    public static class X32 extends TileExpertAssembler {
        @Override
        protected int getSpeedMultiplier() {
            return 32;
        }
    }
}

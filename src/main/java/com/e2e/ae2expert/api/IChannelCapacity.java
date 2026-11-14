package com.e2e.ae2expert.api;

/**
 * Implemented by parts that carry a non standard amount of channels.
 *
 * <p>AE2 rv6 hard codes the channel capacity of a grid node to 8 (normal cable) or 32 (dense
 * cable) inside {@code GridNode.getMaxChannels()}. A tiny ASM patch routes that call through
 * {@code ChannelHook}, which asks the owning part for its capacity through this interface. Parts
 * that do not implement it keep the vanilla behaviour.</p>
 */
public interface IChannelCapacity {

    /** Maximum amount of channels this part can carry. */
    int getChannelCapacity();
}

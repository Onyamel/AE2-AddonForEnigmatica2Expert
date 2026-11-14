package com.e2e.ae2expert.core;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * Coremod entry point of the addon.
 *
 * <p>Two very narrow patches are applied to Applied Energistics 2:</p>
 * <ol>
 *   <li>{@code GridNode.getMaxChannels()} is routed through {@link ChannelHook} so a cable part can
 *       advertise its own channel capacity (AE2 hard codes 8 / 32).</li>
 *   <li>{@code PartCable}'s constructor casts the item to {@code ItemPart} to read the cable colour;
 *       the cast is relaxed so the addon's own cable item works.</li>
 * </ol>
 *
 * <p>Both patches are defensive: if anything goes wrong the original class is returned untouched.</p>
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("AE2ExpertCore")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"com.e2e.ae2expert.core"})
public class ExpertLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "com.e2e.ae2expert.core.ExpertTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

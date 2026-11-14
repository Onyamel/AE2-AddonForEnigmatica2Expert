package com.e2e.ae2expert.crafting;

import java.util.Set;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.crafting.CraftingLink;

public final class CpuDiscovery {
    private CpuDiscovery() { }

    public static void addExpertCpus(CraftingGridCache cache, IGrid grid, Set<CraftingCPUCluster> clusters) {
        for (IGridNode node : grid.getMachines(TileExpertCpu.class)) {
            add(cache, clusters, (TileExpertCpu) node.getMachine());
        }
        for (IGridNode node : grid.getMachines(TileCpuRack.class)) {
            add(cache, clusters, (TileCpuRack) node.getMachine());
        }
    }

    private static void add(CraftingGridCache cache, Set<CraftingCPUCluster> clusters, TileExpertCpu tile) {
        if (!tile.isStorage() || !(tile.getCluster() instanceof CraftingCPUCluster)) return;
        CraftingCPUCluster cluster = (CraftingCPUCluster) tile.getCluster();
        if (clusters.add(cluster) && cluster.getLastCraftingLink() instanceof CraftingLink) {
            cache.addLink((CraftingLink) cluster.getLastCraftingLink());
        }
    }
}

package com.e2e.ae2expert.extended;

import appeng.block.AEBaseTileBlock;
import com.e2e.ae2expert.ModItems;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;

public final class BlockTableAssembler extends AEBaseTileBlock {
    final int width;

    BlockTableAssembler(String tier, int width) {
        super(Material.IRON);
        this.width = width;
        setRegistryName("ae2expert", "table_assembler_" + tier);
        setTranslationKey("ae2expert.table_assembler_" + tier);
        setCreativeTab(ModItems.TAB);
        setTileEntity(TileTableAssembler.class);
        setHardness(3);
        setResistance(12);
        setOpaque(false);
    }

    @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
}

package com.e2e.ae2expert.assembler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;

import appeng.block.crafting.BlockMolecularAssembler;
import appeng.tile.AEBaseTile;

import com.e2e.ae2expert.ModItems;

/**
 * Molecular assembler block of the addon. Behaviour lives entirely in {@link TileExpertAssembler};
 * the block only carries its tier and the matching tile entity class.
 */
public class BlockExpertAssembler extends BlockMolecularAssembler {

    private final AssemblerTier tier;
    private final Class<? extends AEBaseTile> tileClass;

    public BlockExpertAssembler(AssemblerTier tier) {
        super();
        this.tier = tier;
        this.tileClass = tileClassFor(tier);
        this.setRegistryName("ae2expert", "assembler_" + tier.id);
        this.setTranslationKey("ae2expert.assembler_" + tier.id);
        this.setHardness(3.0F);
        this.setResistance(12.0F);
        this.setCreativeTab(ModItems.TAB);
        this.setTileEntity(this.tileClass);
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    public AssemblerTier getTier() {
        return this.tier;
    }

    public Class<? extends AEBaseTile> getTileClass() {
        return this.tileClass;
    }

    /** Maps a tier to its dedicated tile entity class. */
    public static Class<? extends AEBaseTile> tileClassFor(AssemblerTier tier) {
        switch (tier) {
            case X2:
                return TileExpertAssembler.X2.class;
            case X4:
                return TileExpertAssembler.X4.class;
            case X8:
                return TileExpertAssembler.X8.class;
            case X16:
                return TileExpertAssembler.X16.class;
            case X32:
            default:
                return TileExpertAssembler.X32.class;
        }
    }
}

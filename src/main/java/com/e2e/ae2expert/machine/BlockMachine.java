package com.e2e.ae2expert.machine;

import com.e2e.ae2expert.AE2ExpertAddon;
import com.e2e.ae2expert.ModItems;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockMachine extends BlockContainer {
    private final boolean converter;

    public BlockMachine(String name, boolean converter) {
        super(Material.IRON);
        this.converter = converter;
        setRegistryName(AE2ExpertAddon.MODID, name);
        setTranslationKey("ae2expert." + name);
        setCreativeTab(ModItems.TAB);
        setHardness(3);
        setResistance(12);
        setHarvestLevel("pickaxe", 1);
    }

    @Override public TileEntity createNewTileEntity(World world, int meta) {
        return converter ? new TileRfEuConverter() : new TileFluixChamber();
    }

    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }

    @Override public boolean isOpaqueCube(IBlockState state) { return converter; }
    @Override public boolean isFullCube(IBlockState state) { return converter; }
    @Override public net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return converter ? net.minecraft.util.BlockRenderLayer.SOLID : net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (converter && player.isSneaking() && tile instanceof TileRfEuConverter) {
                ((TileRfEuConverter) tile).toggleMode();
            } else {
                player.openGui(AE2ExpertAddon.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileFluixChamber) ((TileFluixChamber) tile).dropContents(null);
        super.breakBlock(world, pos, state);
    }
}

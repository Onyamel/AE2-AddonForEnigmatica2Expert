package com.e2e.ae2expert.extended;

import com.e2e.ae2expert.AE2ExpertAddon;
import com.e2e.ae2expert.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockTableTerminal extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    private static final AxisAlignedBB[] BOUNDS = {
        new AxisAlignedBB(.125, .8125, .125, .875, 1, .875),
        new AxisAlignedBB(.125, 0, .125, .875, .1875, .875),
        new AxisAlignedBB(.125, .125, .8125, .875, .875, 1),
        new AxisAlignedBB(.125, .125, 0, .875, .875, .1875),
        new AxisAlignedBB(.8125, .125, .125, 1, .875, .875),
        new AxisAlignedBB(0, .125, .125, .1875, .875, .875)
    };
    final int width;

    BlockTableTerminal(String tier, int width) {
        super(Material.IRON);
        this.width = width;
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
        setRegistryName("ae2expert", "table_terminal_" + tier);
        setTranslationKey("ae2expert.table_terminal_" + tier);
        setCreativeTab(ModItems.TAB);
        setHardness(3);
        setResistance(12);
    }

    @Override protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override public IBlockState getStateFromMeta(int meta) {
        // Metadata 0 was the only value used by the original full-block terminal.
        EnumFacing face = meta >= 1 && meta <= 6 ? EnumFacing.byIndex(meta - 1) : EnumFacing.SOUTH;
        return getDefaultState().withProperty(FACING, face);
    }

    @Override public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() + 1;
    }

    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing face,
            float x, float y, float z, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, face);
    }

    @Override public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public IBlockState withMirror(IBlockState state, net.minecraft.util.Mirror mirror) {
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BOUNDS[state.getValue(FACING).getIndex()];
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND)
            player.openGui(AE2ExpertAddon.instance, 4, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}

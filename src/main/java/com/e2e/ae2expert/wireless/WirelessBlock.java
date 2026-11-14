package com.e2e.ae2expert.wireless;

import appeng.block.AEBaseTileBlock;
import com.e2e.ae2expert.AE2ExpertAddon;
import com.e2e.ae2expert.ModItems;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class WirelessBlock extends AEBaseTileBlock {
    final int channels;
    final boolean hub;

    WirelessBlock(String id, int channels, boolean hub) {
        super(Material.IRON);
        this.channels = channels;
        this.hub = hub;
        setRegistryName("ae2expert", id);
        setTranslationKey("ae2expert." + id);
        setCreativeTab(ModItems.TAB);
        setHardness(3);
        setResistance(12);
        setTileEntity(WirelessTile.class);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof WirelessTile && placer instanceof EntityPlayer)
            ((WirelessTile) tile).setOwner((EntityPlayer) placer);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!hub || player.getHeldItem(hand).getItem() == WirelessRegistry.LINKER) return false;
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof WirelessTile && ((WirelessTile) tile).mayOpen(player))
            player.openGui(AE2ExpertAddon.instance, 3, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}

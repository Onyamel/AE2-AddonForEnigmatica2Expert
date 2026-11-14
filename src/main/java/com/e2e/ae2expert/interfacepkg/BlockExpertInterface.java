package com.e2e.ae2expert.interfacepkg;

import appeng.block.AEBaseTileBlock;
import appeng.tile.AEBaseTile;
import com.e2e.ae2expert.AE2ExpertAddon;
import com.e2e.ae2expert.ModItems;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockExpertInterface extends AEBaseTileBlock {
    public final int slots;
    public BlockExpertInterface(int slots, Class<? extends AEBaseTile> tile) {
        super(Material.IRON);
        this.slots = slots;
        setRegistryName("ae2expert", "interface_" + slots);
        setTranslationKey("ae2expert.interface_" + slots);
        setCreativeTab(ModItems.TAB);
        setHardness(3);
        setResistance(12);
        setTileEntity(tile);
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!world.isRemote) {
            TileExpertInterface tile = (TileExpertInterface) world.getTileEntity(pos);
            if (tile != null && tile.canUse(player))
                player.openGui(AE2ExpertAddon.instance, 1, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}

package com.e2e.ae2expert.crafting;

import appeng.block.crafting.BlockCraftingUnit;
import com.e2e.ae2expert.AE2ExpertAddon;
import com.e2e.ae2expert.ModItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = AE2ExpertAddon.MODID)
public final class BlockExpertCpu extends BlockCraftingUnit {
    final int memory;
    final int processors;

    BlockExpertCpu(String name, int memory, int processors, boolean rack) {
        super(CraftingUnitType.UNIT);
        this.memory = memory;
        this.processors = processors;
        setRegistryName("ae2expert", name);
        setTranslationKey("ae2expert." + name);
        setCreativeTab(ModItems.TAB);
        setHardness(3);
        setResistance(12);
        setTileEntity(rack ? TileCpuRack.class : TileExpertCpu.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRackSneakClick(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (!player.isSneaking() || world.getBlockState(pos).getBlock() != CpuRegistry.RACK) return;
        TileEntity tile = world.getTileEntity(pos);
        if (world.isRemote) {
            // Let the client send its normal right-click packet even with an item in hand.
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
            return;
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        if (tile instanceof TileCpuRack && ((TileCpuRack) tile).canUse(player)) {
            player.openGui(AE2ExpertAddon.instance, 2, world, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float x, float y, float z) {
        TileEntity tile = world.getTileEntity(pos);
        if (player.isSneaking() && tile instanceof TileCpuRack) {
            if (!world.isRemote && ((TileCpuRack) tile).canUse(player)) {
                player.openGui(AE2ExpertAddon.instance, 2, world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, side, x, y, z);
    }
}

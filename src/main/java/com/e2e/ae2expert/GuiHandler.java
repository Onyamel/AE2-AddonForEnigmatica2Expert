package com.e2e.ae2expert;

import com.e2e.ae2expert.interfacepkg.ContainerExpertInterface;
import com.e2e.ae2expert.interfacepkg.TileExpertInterface;
import com.e2e.ae2expert.machine.ContainerMachine;
import com.e2e.ae2expert.machine.TileFluixChamber;
import com.e2e.ae2expert.machine.TileRfEuConverter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == 0 && (tile instanceof TileFluixChamber || tile instanceof TileRfEuConverter)) return new ContainerMachine(player, tile);
        if (id == 1 && tile instanceof TileExpertInterface && ((TileExpertInterface) tile).canUse(player))
            return new ContainerExpertInterface(player, (TileExpertInterface) tile);
        return null;
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return AE2ExpertAddon.proxy.createGui(id, player, world.getTileEntity(new BlockPos(x, y, z)));
    }
}

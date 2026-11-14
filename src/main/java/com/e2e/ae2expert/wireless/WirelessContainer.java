package com.e2e.ae2expert.wireless;

import java.util.Collections;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;

public final class WirelessContainer extends Container {
    final WirelessTile hub;
    private final EntityPlayer player;
    private int counter;
    public List<WirelessTile.Link> entries = Collections.emptyList();

    public WirelessContainer(EntityPlayer player, WirelessTile hub) {
        this.player = player;
        this.hub = hub;
    }

    public BlockPos position() { return hub.getPos(); }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return player.world.isRemote || hub.mayOpen(player);
    }

    @Override public void detectAndSendChanges() {
        if (player instanceof EntityPlayerMP && counter++ % 10 == 0 && canInteractWith(player)) {
            WirelessPackets.CHANNEL.sendTo(new WirelessPackets.ListMessage(windowId, hub.snapshot()), (EntityPlayerMP) player);
        }
    }
}

package com.e2e.ae2expert.part;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.util.AEPartLocation;
import appeng.api.util.AEColor;
import appeng.api.util.IReadOnlyCollection;
import appeng.me.GridAccessException;
import appeng.parts.networking.PartDenseCableSmart;

import com.e2e.ae2expert.api.IChannelCapacity;
import com.e2e.ae2expert.item.ItemExpertCable;

import io.netty.buffer.ByteBuf;

/**
 * Channel cable part of the addon.
 *
 * <p>Inherits everything from AE2's dense smart cable - connection handling, collision boxes,
 * WAILA/TOP channel display and the cable bus renderer - and only contributes a different channel
 * capacity, which the coremod reads through {@link IChannelCapacity}.</p>
 */
public class PartExpertCable extends PartDenseCableSmart implements IChannelCapacity {

    private final int capacity;
    private final int paint;
    private final int[] actualClientChannels = new int[6];

    public PartExpertCable(ItemStack is) {
        super(is);
        int resolved = 32;
        if (is.getItem() instanceof ItemExpertCable) {
            resolved = ((ItemExpertCable) is.getItem()).getChannelCapacity();
        }
        this.capacity = resolved;
        this.paint = is.getItem() instanceof ItemExpertCable
                ? ((ItemExpertCable) is.getItem()).paintColor(is.getMetadata(), -1) : -1;
        // Цвет обозначает тир, а не изолированную цветовую сеть AE2.
        this.getProxy().setColor(AEColor.TRANSPARENT);
    }

    // Нейтральный корпус; цвет тира накладывается только на индикаторы при выпекании граней.
    @Override public AEColor getCableColor() { return AEColor.GRAY; }

    @Override public boolean changeColor(AEColor color, net.minecraft.entity.player.EntityPlayer player) {
        // Штатная перекраска заменяет часть ванильным кабелем и теряет ёмкость.
        return false;
    }

    @Override
    public int getChannelCapacity() {
        return this.capacity;
    }

    public int getPaint() { return paint; }

    public int getDisplayedUsedChannels() {
        IGridNode node = getGridNode();
        int used = 0;
        if (node != null) {
            for (IGridConnection connection : node.getConnections()) used = Math.max(used, connection.getUsedChannels());
        } else for (int count : actualClientChannels) used = Math.max(used, count);
        return used;
    }

    @Override public void writeToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("usedChannels", getDisplayedUsedChannels());
        tag.setInteger("expertChannelCapacity", capacity);
    }

    @Override public boolean readFromStream(ByteBuf data) throws IOException {
        boolean changed = super.readFromStream(data);
        for (int side = 0; side < 6; side++) {
            int count = data.readUnsignedShort();
            changed |= actualClientChannels[side] != count;
            actualClientChannels[side] = count;
        }
        return changed;
    }

    public static int displaySegments(int used, int capacity) {
        return used <= 0 ? 0 : Math.min(32, (used * 32 + capacity - 1) / capacity);
    }

    /** Шкала AE2 имеет 32 ступени; точное количество передаётся отдельно без переполнения байта. */
    @Override
    public void writeToStream(ByteBuf data) throws IOException {
        int flags = 0;
        boolean[] writeSide = new boolean[EnumFacing.values().length];
        int[] channelsPerSide = new int[EnumFacing.values().length];

        for (EnumFacing thisSide : EnumFacing.values()) {
            IPart part = this.getHost().getPart(thisSide);
            if (part == null) {
                continue;
            }
            writeSide[thisSide.ordinal()] = true;
            int channels = 0;
            IGridNode partNode = part.getGridNode();
            if (partNode != null) {
                IReadOnlyCollection<IGridConnection> connections = partNode.getConnections();
                for (IGridConnection gc : connections) {
                    channels = Math.max(channels, gc.getUsedChannels());
                }
            }
            channelsPerSide[thisSide.ordinal()] = channels;
        }

        IGridNode node = this.getGridNode();
        if (node != null) {
            for (IGridConnection gc : node.getConnections()) {
                AEPartLocation side = gc.getDirection(node);
                if (side == AEPartLocation.INTERNAL) {
                    continue;
                }
                writeSide[side.ordinal()] = true;
                channelsPerSide[side.ordinal()] = gc.getUsedChannels();
                flags |= 1 << side.ordinal();
            }
        }

        try {
            if (this.getProxy().getEnergy().isNetworkPowered()) {
                flags |= 1 << AEPartLocation.INTERNAL.ordinal();
            }
        } catch (GridAccessException ignored) {
            // not connected yet, just report unpowered
        }

        data.writeByte((byte) flags);
        for (int i = 0; i < writeSide.length; i++) {
            if (writeSide[i]) {
                data.writeByte(displaySegments(channelsPerSide[i], capacity));
            }
        }
        // Точные числа отделены от 32 ступеней геометрического индикатора AE2.
        for (int count : channelsPerSide) data.writeShort(Math.max(0, Math.min(65535, count)));
    }
}

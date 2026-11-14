package com.e2e.ae2expert.wireless;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.e2e.ae2expert.AE2ExpertAddon;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class WirelessPackets {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ae2expert_radio");
    private WirelessPackets() { }

    static void register() {
        CHANNEL.registerMessage(ActionHandler.class, Action.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ListHandler.class, ListMessage.class, 1, Side.CLIENT);
    }

    public static final class Action implements IMessage {
        int window;
        boolean unlink;
        BlockPos pos;
        UUID id;
        String name;

        public Action() { }
        public Action(int window, WirelessTile.Link link, boolean unlink, String name) {
            this.window = window;
            this.unlink = unlink;
            pos = link.pos;
            id = link.identity;
            this.name = name;
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeInt(window).writeBoolean(unlink).writeLong(pos.toLong());
            writeId(buf, id);
            ByteBufUtils.writeUTF8String(buf, name);
        }
        @Override public void fromBytes(ByteBuf buf) {
            window = buf.readInt();
            unlink = buf.readBoolean();
            pos = BlockPos.fromLong(buf.readLong());
            id = readId(buf);
            name = ByteBufUtils.readUTF8String(buf);
        }
    }

    public static final class ActionHandler implements IMessageHandler<Action, IMessage> {
        @Override public IMessage onMessage(Action message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.name.length() > 40 || !(player.openContainer instanceof WirelessContainer)) return;
                WirelessContainer container = (WirelessContainer) player.openContainer;
                if (container.windowId != message.window || !container.canInteractWith(player)) return;
                if (message.unlink) container.hub.unlink(message.pos, message.id);
                else container.hub.rename(message.pos, message.id, message.name);
                CHANNEL.sendTo(new ListMessage(container.windowId, container.hub.snapshot()), player);
            });
            return null;
        }
    }

    public static final class ListMessage implements IMessage {
        public int window;
        public List<WirelessTile.Link> links;

        public ListMessage() { }
        ListMessage(int window, List<WirelessTile.Link> links) { this.window = window; this.links = links; }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeInt(window).writeShort(links.size());
            for (WirelessTile.Link link : links) {
                buf.writeLong(link.pos.toLong());
                writeId(buf, link.identity);
                ByteBufUtils.writeUTF8String(buf, link.name);
                buf.writeShort(link.channels).writeBoolean(link.online);
            }
        }
        @Override public void fromBytes(ByteBuf buf) {
            window = buf.readInt();
            int count = buf.readUnsignedShort();
            if (count > WirelessTile.MAX_LINKS) throw new IllegalArgumentException("Too many wireless links");
            links = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                BlockPos pos = BlockPos.fromLong(buf.readLong());
                UUID id = readId(buf);
                String name = ByteBufUtils.readUTF8String(buf);
                links.add(new WirelessTile.Link(pos, id, name, buf.readUnsignedShort(), buf.readBoolean()));
            }
        }
    }

    public static final class ListHandler implements IMessageHandler<ListMessage, IMessage> {
        @Override public IMessage onMessage(ListMessage message, MessageContext context) {
            AE2ExpertAddon.proxy.receiveWirelessList(message);
            return null;
        }
    }

    private static void writeId(ByteBuf buf, UUID id) {
        buf.writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits());
    }
    private static UUID readId(ByteBuf buf) { return new UUID(buf.readLong(), buf.readLong()); }
}

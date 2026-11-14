package com.e2e.ae2expert.wireless;

import java.util.*;
import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkTile;
import com.e2e.ae2expert.api.IChannelCapacity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class WirelessTile extends AENetworkTile implements ITickable, IChannelCapacity {
    public static final int MAX_LINKS = 128;
    private UUID identity = UUID.randomUUID();
    private UUID owner;
    private BlockPos hubPos;
    private UUID hubId;
    private final Map<BlockPos, Link> links = new LinkedHashMap<>();
    private IGridConnection radio;
    private int tick;
    private int shownLinks;

    public WirelessTile() {
        getProxy().setFlags(GridFlags.DENSE_CAPACITY);
        getProxy().setIdlePowerUsage(2);
    }

    public static final class Link {
        public final BlockPos pos;
        public final UUID identity;
        public final String name;
        public final int channels;
        public final boolean online;

        Link(BlockPos pos, UUID identity, String name, int channels, boolean online) {
            this.pos = pos;
            this.identity = identity;
            this.name = name;
            this.channels = channels;
            this.online = online;
        }
    }

    @Override
    protected ItemStack getItemFromTile(Object tile) {
        return world == null ? ItemStack.EMPTY : new ItemStack(getBlockType());
    }

    @Override public void onReady() {
        super.onReady();
        getProxy().setVisualRepresentation(getItemFromTile(this));
    }

    @Override public AECableType getCableConnectionType(AEPartLocation side) { return AECableType.DENSE_SMART; }

    public boolean isHub() { return getBlockType() == WirelessRegistry.HUB; }
    public UUID identity() { return identity; }
    public int connectionCount() { return world != null && world.isRemote ? shownLinks : links.size(); }

    @Override public int getChannelCapacity() {
        Block block = getBlockType();
        return block instanceof WirelessBlock ? ((WirelessBlock) block).channels : 0;
    }

    void setOwner(EntityPlayer player) {
        owner = player.getUniqueID();
        getProxy().setOwner(player);
        markDirty();
    }

    boolean mayManage(EntityPlayer player) {
        if (world == null || world != player.world || isInvalid() || !world.isBlockLoaded(pos) || world.getTileEntity(pos) != this)
            return false;
        if (owner != null && !owner.equals(player.getUniqueID())) return false;
        try { return getProxy().getSecurity().hasPermission(player, SecurityPermissions.BUILD); }
        catch (GridAccessException offline) { return owner != null; }
    }

    public boolean mayOpen(EntityPlayer player) {
        return isHub() && player.getDistanceSqToCenter(pos) <= 64 && mayManage(player);
    }

    static WirelessTile loaded(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return null;
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof WirelessTile && !tile.isInvalid() ? (WirelessTile) tile : null;
    }

    boolean bind(EntityPlayer player, WirelessTile relay) {
        if (world == null || world.isRemote || !isHub() || relay.isHub() || world != relay.world
                || !mayManage(player) || !relay.mayManage(player)) return false;
        Link previous = links.get(relay.pos);
        if (previous == null && links.size() >= MAX_LINKS) return false;
        if (previous != null && previous.identity.equals(relay.identity)
                && identity.equals(relay.hubId) && pos.equals(relay.hubPos)) return true;
        // Require an explicit unlink before moving a relay to another hub.
        if (relay.hubId != null && !identity.equals(relay.hubId)) {
            if (!world.isBlockLoaded(relay.hubPos)) return false;
            WirelessTile old = loaded(world, relay.hubPos);
            if (old != null && relay.hubId.equals(old.identity)) return false;
            relay.clearBinding();
        }
        relay.hubPos = pos.toImmutable();
        relay.hubId = identity;
        relay.markDirty();
        links.put(relay.pos.toImmutable(), new Link(relay.pos.toImmutable(), relay.identity,
                previous != null && previous.identity.equals(relay.identity) ? previous.name : "",
                relay.getChannelCapacity(), false));
        changed();
        relay.refreshRadio();
        return true;
    }

    void rename(BlockPos target, UUID expected, String name) {
        Link link = links.get(target);
        if (link == null || !link.identity.equals(expected)) return;
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        if (clean.length() > 40) clean = clean.substring(0, 40);
        if (clean.equals(link.name)) return;
        links.put(target, new Link(target, expected, clean, link.channels, false));
        markDirty();
    }

    void unlink(BlockPos target, UUID expected) {
        Link link = links.get(target);
        if (link == null || !link.identity.equals(expected)) return;
        links.remove(target);
        WirelessTile relay = loaded(world, target);
        if (relay != null && expected.equals(relay.identity) && identity.equals(relay.hubId)) relay.clearBinding();
        changed();
    }

    void unlinkRelay(EntityPlayer player) {
        if (hubId == null || !mayManage(player)) return;
        WirelessTile hub = loaded(world, hubPos);
        if (hub != null && hubId.equals(hub.identity)) hub.unlink(pos, identity);
        clearBinding();
    }

    private void clearBinding() {
        dropRadio();
        hubPos = null;
        hubId = null;
        markDirty();
    }

    private void changed() {
        markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
    }

    public List<Link> snapshot() {
        List<Link> result = new ArrayList<>(links.size());
        for (Link link : links.values()) {
            WirelessTile relay = loaded(world, link.pos);
            boolean online = relay != null && link.identity.equals(relay.identity) && relay.liveRadio();
            result.add(new Link(link.pos, link.identity, link.name, link.channels, online));
        }
        return result;
    }

    @Override public void update() {
        if (world.isRemote || ++tick % 20 != 0) return;
        if (!isHub()) {
            refreshRadio();
            return;
        }
        boolean removed = links.values().removeIf(link -> {
            if (!world.isBlockLoaded(link.pos)) return false;
            WirelessTile relay = loaded(world, link.pos);
            if (relay == null) return !(world.getBlockState(link.pos).getBlock() instanceof WirelessBlock);
            return !link.identity.equals(relay.identity)
                    || !identity.equals(relay.hubId) || !pos.equals(relay.hubPos);
        });
        if (removed) changed();
    }

    private boolean liveRadio() {
        if (radio == null) return false;
        IGridNode node = getProxy().getNode();
        return node != null && node.getConnections().contains(radio);
    }

    private void refreshRadio() {
        if (hubId == null || !world.isBlockLoaded(hubPos)) {
            dropRadio();
            return;
        }
        WirelessTile hub = loaded(world, hubPos);
        if (hub == null && world.getBlockState(hubPos).getBlock() == WirelessRegistry.HUB) {
            dropRadio();
            return;
        }
        Link entry = hub == null ? null : hub.links.get(pos);
        if (hub == null || !hubId.equals(hub.identity) || entry == null || !identity.equals(entry.identity)) {
            clearBinding();
            return;
        }
        if (liveRadio()) return;
        radio = null;
        IGridNode a = hub.getProxy().getNode(), b = getProxy().getNode();
        if (a == null || b == null) return;
        try { radio = AEApi.instance().grid().createGridConnection(a, b); }
        catch (FailedConnectionException rejected) { /* AE2 enforces its normal network security. */ }
    }

    private void dropRadio() {
        if (liveRadio()) radio.destroy();
        radio = null;
    }

    @Override public void onChunkUnload() {
        dropRadio();
        super.onChunkUnload();
    }

    @Override public void invalidate() {
        dropRadio();
        super.invalidate();
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setUniqueId("radioId", identity);
        if (owner != null) tag.setUniqueId("radioOwner", owner);
        if (hubId != null) {
            tag.setUniqueId("hubId", hubId);
            tag.setLong("hubPos", hubPos.toLong());
        }
        NBTTagList entries = new NBTTagList();
        for (Link link : links.values()) {
            NBTTagCompound saved = new NBTTagCompound();
            saved.setLong("pos", link.pos.toLong());
            saved.setUniqueId("id", link.identity);
            saved.setString("name", link.name);
            saved.setInteger("tier", link.channels);
            entries.appendTag(saved);
        }
        tag.setTag("radioLinks", entries);
        return tag;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasUniqueId("radioId")) identity = tag.getUniqueId("radioId");
        owner = tag.hasUniqueId("radioOwner") ? tag.getUniqueId("radioOwner") : null;
        hubId = tag.hasUniqueId("hubId") ? tag.getUniqueId("hubId") : null;
        hubPos = hubId == null ? null : BlockPos.fromLong(tag.getLong("hubPos"));
        links.clear();
        NBTTagList entries = tag.getTagList("radioLinks", 10);
        for (int i = 0; i < Math.min(MAX_LINKS, entries.tagCount()); i++) {
            NBTTagCompound saved = entries.getCompoundTagAt(i);
            if (!saved.hasUniqueId("id")) continue;
            BlockPos target = BlockPos.fromLong(saved.getLong("pos"));
            String name = saved.getString("name");
            links.put(target, new Link(target, saved.getUniqueId("id"), name.substring(0, Math.min(40, name.length())),
                    saved.getInteger("tier"), false));
        }
    }

    @Override public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "ae2expert:wireless_node");
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
        tag.setInteger("count", links.size());
        return tag;
    }

    @Override public void handleUpdateTag(NBTTagCompound tag) { shownLinks = tag.getInteger("count"); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) { handleUpdateTag(packet.getNbtCompound()); }
}

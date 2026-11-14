package com.e2e.ae2expert.core;

import java.lang.reflect.Field;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridConnection;
import appeng.api.util.AEColor;
import appeng.items.parts.ItemPart;

import com.e2e.ae2expert.api.IChannelCapacity;
import com.e2e.ae2expert.api.ICableColorItem;

/**
 * Runtime side of the coremod patches.
 *
 * <p>This class is referenced from patched AE2 bytecode, therefore every method must stay defensive:
 * any unexpected state falls back to vanilla behaviour instead of throwing inside the network
 * pathfinder.</p>
 */
public final class ChannelHook {

    /** Mirrors AE2's private {@code CHANNEL_COUNT} array. */
    private static final int[] VANILLA_CAPACITY = { 0, 8, 32 };

    private static Field compressedDataField;
    private static boolean compressedDataResolved;

    private ChannelHook() {
    }

    /**
     * Called instead of AE2's {@code GridNode.getMaxChannels()}.
     *
     * @param node the {@code appeng.me.GridNode} instance (typed as Object to keep the patch simple)
     * @return channel capacity for this node
     */
    public static int getMaxChannels(Object node) {
        try {
            if (node instanceof IGridNode) {
                IGridNode gridNode = (IGridNode) node;
                if (gridNode.getMachine() instanceof IChannelCapacity) {
                    int capacity = ((IChannelCapacity) gridNode.getMachine()).getChannelCapacity();
                    return Math.max(0, capacity);
                }
                return vanillaCapacity(gridNode);
            }
        } catch (Throwable t) {
            logOnce("getMaxChannels", t);
        }
        return 8;
    }

    private static volatile Field connectionData;

    private static Field connectionData() throws ReflectiveOperationException {
        Field result = connectionData;
        if (result == null) {
            synchronized (ChannelHook.class) {
                result = connectionData;
                if (result == null) {
                    result = Class.forName("appeng.me.GridConnection").getDeclaredField("channelData");
                    result.setAccessible(true);
                    connectionData = result;
                }
            }
        }
        return result;
    }

    /** Текущий счётчик соединения расширен ASM-патчем до 16 бит. */
    public static boolean connectionCanCarry(Object object) {
        try {
            IGridConnection connection = (IGridConnection) object;
            Field counter = connectionData();
            int capacity = 32;
            Object a = connection.a().getMachine(), b = connection.b().getMachine();
            boolean customA = a instanceof IChannelCapacity, customB = b instanceof IChannelCapacity;
            if (customA || customB) {
                int ca = customA ? ((IChannelCapacity) a).getChannelCapacity()
                        : a instanceof appeng.tile.networking.TileController ? 256 : getMaxChannels(connection.a());
                int cb = customB ? ((IChannelCapacity) b).getChannelCapacity()
                        : b instanceof appeng.tile.networking.TileController ? 256 : getMaxChannels(connection.b());
                capacity = Math.min(ca, cb);
            }
            return (counter.getInt(object) & 0xffff) < capacity;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logOnce("getMaxChannels", failure);
            return false;
        }
    }

    /** Exact reproduction of the vanilla logic, used for every non addon node. */
    private static int vanillaCapacity(IGridNode node) {
        Field field = compressedData();
        if (field != null) {
            try {
                return VANILLA_CAPACITY[field.getInt(node) & 3];
            } catch (Throwable ignored) {
                // fall through to the flag based reconstruction
            }
        }
        if (node.hasFlag(GridFlags.CANNOT_CARRY)) {
            return 0;
        }
        if (node.hasFlag(GridFlags.DENSE_CAPACITY)) {
            return 32;
        }
        return 8;
    }

    private static synchronized Field compressedData() {
        if (!compressedDataResolved) {
            compressedDataResolved = true;
            try {
                Field field = Class.forName("appeng.me.GridNode").getDeclaredField("compressedData");
                field.setAccessible(true);
                compressedDataField = field;
            } catch (Throwable ignored) {
                compressedDataField = null;
            }
        }
        return compressedDataField;
    }

    /**
     * Called instead of {@code ItemPart.variantOf(int)} inside {@code PartCable}'s constructor.
     *
     * @return index into {@code AEColor.values()}
     */
    public static int variantOf(Object item, int meta) {
        try {
            if (item instanceof ItemPart) {
                return ((ItemPart) item).variantOf(meta);
            }
            if (item instanceof ICableColorItem) {
                AEColor color = ((ICableColorItem) item).getCableColor();
                return color == null ? 0 : color.ordinal();
            }
        } catch (Throwable t) {
            logOnce("variantOf", t);
        }
        return 0;
    }

    private static boolean loggedMax;
    private static boolean loggedVariant;

    private static synchronized void logOnce(String where, Throwable t) {
        if ("getMaxChannels".equals(where)) {
            if (loggedMax) {
                return;
            }
            loggedMax = true;
        } else {
            if (loggedVariant) {
                return;
            }
            loggedVariant = true;
        }
        System.err.println("[AE2Expert] ChannelHook." + where + " failed, using fallback: " + t);
    }
}

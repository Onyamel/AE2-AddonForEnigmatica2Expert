package com.e2e.ae2expert.core;

import appeng.api.parts.IPart;
import appeng.parts.networking.PartCableSmart;
import appeng.parts.networking.PartDenseCableSmart;
import com.e2e.ae2expert.api.IChannelCapacity;
import com.e2e.ae2expert.part.PartExpertCable;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTTagCompound;

/** Общая целочисленная телеметрия, не участвующая в распределении каналов. */
public final class DisplayChannels {
    private static final Map<IPart, Integer> LAST = Collections.synchronizedMap(new WeakHashMap<IPart, Integer>());
    private DisplayChannels() { }
    public static int capacity(IPart part) {
        return part instanceof IChannelCapacity ? ((IChannelCapacity)part).getChannelCapacity()
                : part instanceof PartDenseCableSmart ? 32 : 8;
    }
    public static NBTTagCompound writeTag(IPart part, NBTTagCompound tag) {
        if (part instanceof PartCableSmart || part instanceof PartDenseCableSmart) {
            NBTTagCompound data = new NBTTagCompound();
            part.writeToNBT(data);
            if (data.hasKey("usedChannels")) tag.setInteger("usedChannels", data.getInteger("usedChannels"));
            tag.setInteger("expertChannelCapacity", capacity(part));
        }
        return tag;
    }
    public static List<String> tooltip(IPart part, List<String> lines, NBTTagCompound data) {
        if (!(part instanceof PartCableSmart || part instanceof PartDenseCableSmart)) return lines;
        Integer count;
        if (data.hasKey("usedChannels")) { count = data.getInteger("usedChannels"); LAST.put(part, count); }
        else count = part instanceof PartExpertCable ? ((PartExpertCable)part).getDisplayedUsedChannels() : LAST.get(part);
        if (count != null) lines.add(String.format(appeng.core.localization.WailaText.Channels.getLocal(), count, capacity(part)));
        return lines;
    }
}

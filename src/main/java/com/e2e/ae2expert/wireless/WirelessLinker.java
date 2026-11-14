package com.e2e.ae2expert.wireless;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import com.e2e.ae2expert.ModItems;

final class WirelessLinker extends Item {
    WirelessLinker() {
        setRegistryName("ae2expert", "wireless_linker");
        setTranslationKey("ae2expert.wireless_linker");
        setCreativeTab(ModItems.TAB);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side,
            float x, float y, float z) {
        WirelessTile tile = WirelessTile.loaded(world, pos);
        if (tile == null) return EnumActionResult.PASS;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.getTagCompound();
        String result = "denied";
        if (tile.mayManage(player)) {
            if (tile.isHub()) {
                tag = new NBTTagCompound();
                tag.setLong("hub", pos.toLong());
                tag.setUniqueId("id", tile.identity());
                tag.setInteger("dimension", world.provider.getDimension());
                stack.setTagCompound(tag);
                result = "selected";
            } else if (player.isSneaking()) {
                tile.unlinkRelay(player);
                result = "unlinked";
            } else if (tag != null && tag.hasUniqueId("id") && tag.getInteger("dimension") == world.provider.getDimension()) {
                WirelessTile hub = WirelessTile.loaded(world, BlockPos.fromLong(tag.getLong("hub")));
                if (hub != null && hub.identity().equals(tag.getUniqueId("id")) && hub.bind(player, tile)) result = "linked";
            }
        }
        player.sendStatusMessage(new TextComponentTranslation("ae2expert.radio." + result), true);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("ae2expert.radio.linker_hint"));
    }
}

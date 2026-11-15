package com.e2e.ae2expert.extended;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class TableRecipeTransfer implements IMessage {
    private static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("ae2expert_table");
    private int window, size;
    private ItemStack result;
    private List<ItemStack> inputs;

    public static void register() {
        NETWORK.registerMessage(Handler.class, TableRecipeTransfer.class, 0, Side.SERVER);
    }

    public static void send(int window, int size, List<ItemStack> inputs, ItemStack result) {
        TableRecipeTransfer request = new TableRecipeTransfer();
        request.window = window;
        request.size = size;
        request.inputs = inputs;
        request.result = result;
        NETWORK.sendToServer(request);
    }

    @Override public void fromBytes(ByteBuf bytes) {
        if (bytes.readableBytes() < 2 || bytes.readableBytes() > 32767) return;
        PacketBuffer buffer = new PacketBuffer(bytes);
        window = buffer.readUnsignedByte();
        size = buffer.readUnsignedByte();
        if (size != 3 && size != 5 && size != 7 && size != 9) return;
        try {
            result = buffer.readItemStack();
            List<ItemStack> slots = new ArrayList<>(size * size);
            for (int i = 0; i < size * size; i++) {
                ItemStack input = buffer.readItemStack();
                if (!input.isEmpty()) input.setCount(1);
                slots.add(input);
            }
            if (!buffer.isReadable()) inputs = slots;
        } catch (IOException | RuntimeException invalid) {
            inputs = null;
        }
    }

    @Override public void toBytes(ByteBuf bytes) {
        PacketBuffer buffer = new PacketBuffer(bytes);
        buffer.writeByte(window);
        buffer.writeByte(size);
        buffer.writeItemStack(result);
        for (ItemStack stack : inputs) buffer.writeItemStack(stack);
    }

    public static final class Handler implements IMessageHandler<TableRecipeTransfer, IMessage> {
        @Override public IMessage onMessage(TableRecipeTransfer request, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            if (request.inputs != null) player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerTableTerminal
                        && player.openContainer.windowId == request.window
                        && player.openContainer.canInteractWith(player)) {
                    ((ContainerTableTerminal) player.openContainer).transferRecipe(request.size, request.inputs, request.result);
                }
            });
            return null;
        }
    }
}

package com.e2e.ae2expert;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class PatternQuantity implements IMessage {
    private static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("ae2expert_qty");
    private int window, slot, count;

    public PatternQuantity() { }

    public static void register() {
        NETWORK.registerMessage(Handler.class, PatternQuantity.class, 0, Side.SERVER);
    }

    public static boolean canEdit(Container container, Slot slot) {
        if (container instanceof com.e2e.ae2expert.extended.ContainerTableTerminal)
            return ((com.e2e.ae2expert.extended.ContainerTableTerminal) container).canEditQuantity(slot);
        return container instanceof ContainerPatternTerm && !((ContainerPatternTerm) container).isCraftingMode()
                && (slot instanceof SlotFakeCraftingMatrix || slot instanceof OptionalSlotFake)
                && ((appeng.container.slot.SlotFake) slot).isSlotEnabled() && slot.getHasStack();
    }

    public static void send(int window, int slot, int count) {
        PatternQuantity message = new PatternQuantity();
        message.window = window;
        message.slot = slot;
        message.count = count;
        NETWORK.sendToServer(message);
    }

    @Override public void fromBytes(ByteBuf buf) {
        window = buf.readInt(); slot = buf.readInt(); count = buf.readInt();
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(window).writeInt(slot).writeInt(count);
    }

    public static final class Handler implements IMessageHandler<PatternQuantity, IMessage> {
        @Override public IMessage onMessage(PatternQuantity message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (container.windowId != message.window || !container.canInteractWith(player)
                        || message.slot < 0 || message.slot >= container.inventorySlots.size()
                        || message.count < 1 || message.count > 64) return;
                Slot slot = container.getSlot(message.slot);
                if (!canEdit(container, slot)) return;
                ItemStack stack = slot.getStack().copy();
                stack.setCount(message.count);
                slot.putStack(stack);
                container.detectAndSendChanges();
            });
            return null;
        }
    }
}

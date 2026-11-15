package com.e2e.ae2expert.client;

import com.e2e.ae2expert.PatternQuantity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public final class PatternQuantityGui extends GuiContainer {
    private final GuiContainer terminal;
    private final Slot slot;
    private GuiTextField quantity;

    private PatternQuantityGui(GuiContainer terminal, Slot slot) {
        super(terminal.inventorySlots);
        this.terminal = terminal;
        this.slot = slot;
    }

    public static final class InputHandler {
        @SubscribeEvent
        public void middleClick(GuiScreenEvent.MouseInputEvent.Pre event) {
            if (!(event.getGui() instanceof GuiContainer) || Mouse.getEventButton() != 2 || !Mouse.getEventButtonState()) return;
            GuiContainer terminal = (GuiContainer) event.getGui();
            Slot slot = terminal.getSlotUnderMouse();
            if (slot == null || !PatternQuantity.canEdit(terminal.inventorySlots, slot)) return;
            event.setCanceled(true);
            Minecraft mc = Minecraft.getMinecraft();
            mc.currentScreen = null;
            mc.displayGuiScreen(new PatternQuantityGui(terminal, slot));
        }

    }

    @Override public void onGuiClosed() { }

    @Override protected void drawGuiContainerBackgroundLayer(float ticks, int x, int y) { }

    @Override public void initGui() {
        buttonList.clear();
        quantity = new GuiTextField(0, fontRenderer, width / 2 - 40, height / 2 - 8, 80, 20);
        quantity.setMaxStringLength(2);
        quantity.setText(Integer.toString(slot.getStack().getCount()));
        quantity.setFocused(true);
        buttonList.add(new AePanel.Action(0, width / 2 - 40, height / 2 + 18, 80, 18, "", I18n.format("gui.done")));
    }

    @Override public void drawScreen(int x, int y, float ticks) {
        drawDefaultBackground();
        AePanel.frame(width / 2 - 83, height / 2 - 42, 166, 88);
        String title = I18n.format("ae2expert.pattern.quantity");
        fontRenderer.drawString(title, (width - fontRenderer.getStringWidth(title)) / 2, height / 2 - 32, AePanel.TEXT);
        AePanel.divider(width / 2 - 74, height / 2 - 19, 148);
        quantity.drawTextBox();
        for (GuiButton button : buttonList) button.drawButton(mc, x, y, ticks);
    }

    @Override protected void mouseClicked(int x, int y, int button) {
        quantity.mouseClicked(x, y, button);
        if (button != 0) return;
        for (GuiButton control : buttonList) {
            if (control.mousePressed(mc, x, y)) actionPerformed(control);
        }
    }

    @Override protected void mouseReleased(int x, int y, int button) { }
    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) { }

    @Override protected void keyTyped(char c, int key) throws java.io.IOException {
        if (key == 1) mc.displayGuiScreen(terminal);
        else if (key == 28 || key == 156) save();
        else quantity.textboxKeyTyped(c, key);
    }

    @Override protected void actionPerformed(GuiButton button) { save(); }

    private void save() {
        try {
            int count = Integer.parseInt(quantity.getText());
            if (count < 1 || count > 64) return;
            PatternQuantity.send(terminal.inventorySlots.windowId, slot.slotNumber, count);
            mc.displayGuiScreen(terminal);
        } catch (NumberFormatException invalid) {
            // Keep the dialog open for correction.
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}

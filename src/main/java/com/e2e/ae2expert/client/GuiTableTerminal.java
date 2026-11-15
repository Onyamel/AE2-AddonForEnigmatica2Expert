package com.e2e.ae2expert.client;

import com.e2e.ae2expert.extended.ContainerTableTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import java.io.IOException;

public final class GuiTableTerminal extends GuiContainer {
    private final ContainerTableTerminal terminal;
    private boolean layoutChanged;

    public GuiTableTerminal(ContainerTableTerminal terminal) {
        super(terminal);
        this.terminal = terminal;

    }

    @Override public void initGui() {
        xSize = terminal.controlsLeft() + 77;
        ySize = terminal.inventoryTop() + 78;
        super.initGui();
        buttonList.clear();
        layoutChanged = false;
        int x = guiLeft + terminal.controlsLeft() + 4;
        int y = guiTop + 20;
        addButton(new AePanel.Action(3, x, y, 64, 16, "", modeAction()));
        addButton(new AePanel.Action(0, x, y + 23, 64, 16, "+", I18n.format("gui.ae2expert.table_encode")));
        addButton(new AePanel.Action(2, x, y + 42, 64, 16, "<", I18n.format("gui.ae2expert.table_load")));
        addButton(new AePanel.Action(1, x, y + 61, 64, 16, "×", I18n.format("gui.ae2expert.table_clear")));
        updateControls();
    }

    private String modeAction() {
        return I18n.format(terminal.isProcessing() ? "gui.ae2expert.table_crafting" : "gui.ae2expert.table_processing");
    }

    private void updateControls() {
        AePanel.Action mode = (AePanel.Action) buttonList.get(0);
        mode.displayString = modeAction();
        mode.selected = terminal.isProcessing();
        buttonList.get(2).enabled = !terminal.isProcessing();
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
        if (button.id == 3) {
            terminal.toggleClientMode();
            layoutChanged = true;
        }
    }

    @Override public void drawScreen(int x, int y, float partialTicks) {
        if (layoutChanged) initGui();
        drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);
        renderHoveredToolTip(x, y);
    }

    @Override protected void drawGuiContainerForegroundLayer(int x, int y) {
        String heading = terminal.isProcessing() ? I18n.format("gui.ae2expert.table_processing")
                : I18n.format("gui.ae2expert.table_title", terminal.width, terminal.width);
        fontRenderer.drawString(heading, 9, 5, AePanel.TEXT);
        if (terminal.isProcessing()) {
            fontRenderer.drawString(I18n.format("gui.ae2expert.table_inputs"), 9, 74, AePanel.TEXT);
            fontRenderer.drawString(I18n.format("gui.ae2expert.table_outputs"), 69, 24, AePanel.TEXT);
        }
        fontRenderer.drawString(I18n.format("gui.ae2expert.table_result"), terminal.controlsLeft() + 3, 102, AePanel.TEXT);
        fontRenderer.drawString(I18n.format("gui.ae2expert.table_written"), terminal.controlsLeft() + 40, 102, AePanel.TEXT);
        String inventory = I18n.format("container.inventory");
        if (xSize - 181 >= fontRenderer.getStringWidth(inventory))
            fontRenderer.drawString(inventory, 175, terminal.inventoryTop() + 1, AePanel.TEXT);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float time, int x, int y) {
        AePanel.frame(guiLeft, guiTop, xSize, ySize);
        AePanel.divider(guiLeft + 7, guiTop + 14, xSize - 14);
        AePanel.inset(guiLeft + terminal.controlsLeft(), guiTop + 17, 72, 113);
        AePanel.divider(guiLeft + 7, guiTop + terminal.inventoryTop() - 5, xSize - 14);
        int index = 0;
        int templateSlots = terminal.width * terminal.width + 5;
        for (net.minecraft.inventory.Slot slot : inventorySlots.inventorySlots) {
            if (slot.isEnabled()) AePanel.slot(guiLeft + slot.xPos, guiTop + slot.yPos, index < templateSlots);
            index++;
        }
    }
}

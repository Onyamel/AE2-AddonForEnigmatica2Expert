package com.e2e.ae2expert.client;

import com.e2e.ae2expert.crafting.ContainerCpuRack;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;

public final class GuiCpuRack extends GuiContainer {
    private final ContainerCpuRack rack;

    public GuiCpuRack(ContainerCpuRack rack) {
        super(rack);
        this.rack = rack;
        ySize = 183;
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);
        renderHoveredToolTip(x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float ticks, int x, int y) {
        AePanel.frame(guiLeft, guiTop, xSize, ySize);
        AePanel.slots(guiLeft, guiTop, rack.inventorySlots);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRenderer.drawString(I18n.format("tile.ae2expert.crafting_cpu_rack.name"), 8, 6, 0x252525);
        fontRenderer.drawString(I18n.format("ae2expert.cpu.memory", rack.bytes / 1024), 8, 17, 0x252525);
        fontRenderer.drawString(I18n.format("ae2expert.cpu.processors", rack.processors), 8, 27, 0x252525);
        if (rack.locked) fontRenderer.drawString(I18n.format("ae2expert.cpu.busy"), 8, 37, 0x822e22);
    }
}

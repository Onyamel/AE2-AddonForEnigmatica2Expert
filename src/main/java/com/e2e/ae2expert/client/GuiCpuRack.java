package com.e2e.ae2expert.client;

import com.e2e.ae2expert.crafting.ContainerCpuRack;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;

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
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xff272b31);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xffc6c6c6);
        for (Slot slot : rack.inventorySlots) {
            int left = guiLeft + slot.xPos, top = guiTop + slot.yPos;
            drawRect(left - 1, top - 1, left + 17, top + 17, 0xff51555b);
            drawRect(left, top, left + 16, top + 16, 0xff8b8b8b);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRenderer.drawString(I18n.format("tile.ae2expert.crafting_cpu_rack.name"), 8, 6, 0x252525);
        fontRenderer.drawString(I18n.format("ae2expert.cpu.memory", rack.bytes / 1024), 8, 17, 0x252525);
        fontRenderer.drawString(I18n.format("ae2expert.cpu.processors", rack.processors), 8, 27, 0x252525);
        if (rack.locked) fontRenderer.drawString(I18n.format("ae2expert.cpu.busy"), 8, 37, 0x822e22);
    }
}

package com.e2e.ae2expert.client;

import com.e2e.ae2expert.interfacepkg.ContainerExpertInterface;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;

/** Окно 246 x 260 помещается при масштабе 4 на экране 1920 x 1080. */
public final class GuiExpertInterface extends GuiContainer {
    private final ContainerExpertInterface container;
    public GuiExpertInterface(ContainerExpertInterface container) {
        super(container);
        this.container = container;
        xSize = 246;
        ySize = 260;
    }
    @Override public void initGui() {
        super.initGui();
        String[] names = {"config", "buffer", "patterns"};
        for (int i = 0; i < 3; i++) buttonList.add(new GuiButton(i, guiLeft + 5 + i * 79, guiTop + 16,
                77, 14, I18n.format("ae2expert.gui." + names[i])));
    }
    @Override protected void actionPerformed(GuiButton button) {
        container.setTab(button.id);
        mc.playerController.sendEnchantPacket(container.windowId, button.id);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        if (mouseX >= guiLeft + 228 && mouseX < guiLeft + 242
                && mouseY >= guiTop + 3 && mouseY < guiTop + 15)
            drawHoveringText(java.util.Arrays.asList(
                    I18n.format(container.active ? "ae2expert.gui.online" : "ae2expert.gui.offline"),
                    I18n.format("ae2expert.gui.queued", container.queuedItems)), mouseX, mouseY);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks, int mouseX, int mouseY) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xff272b31);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xffc6c6c6);
        for (Slot slot : inventorySlots.inventorySlots) {
            if (!slot.isEnabled()) continue;
            int x = guiLeft + slot.xPos, y = guiTop + slot.yPos;
            drawRect(x - 1, y - 1, x + 17, y + 17, 0xff555555);
            drawRect(x, y, x + 16, y + 16, 0xff8b8b8b);
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("tile.ae2expert.interface_" + container.tile.getSize() + ".name"), 6, 5, 0x252525);
        drawRect(230, 5, 239, 13, container.active ? 0xff32aa64 : 0xffaa4444);
    }
}

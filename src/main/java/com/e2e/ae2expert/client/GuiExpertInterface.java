package com.e2e.ae2expert.client;

import com.e2e.ae2expert.interfacepkg.ContainerExpertInterface;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;

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
        for (int i = 0; i < 3; i++) buttonList.add(new AePanel.Action(i, guiLeft + 5 + i * 79, guiTop + 16,
                77, 14, "", I18n.format("ae2expert.gui." + names[i])));
    }
    @Override protected void actionPerformed(GuiButton button) {
        container.setTab(button.id);
        mc.playerController.sendEnchantPacket(container.windowId, button.id);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : buttonList) ((AePanel.Action) button).selected = button.id == container.tab;
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
        AePanel.frame(guiLeft, guiTop, xSize, ySize);
        AePanel.slots(guiLeft, guiTop, inventorySlots.inventorySlots);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("tile.ae2expert.interface_" + container.tile.getSize() + ".name"), 6, 5, 0x252525);
        drawRect(230, 5, 239, 13, container.active ? 0xff32aa64 : 0xffaa4444);
    }
}

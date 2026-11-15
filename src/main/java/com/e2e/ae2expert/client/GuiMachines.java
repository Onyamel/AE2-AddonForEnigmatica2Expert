package com.e2e.ae2expert.client;

import com.e2e.ae2expert.machine.ContainerMachine;
import com.e2e.ae2expert.machine.TileRfEuConverter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;

public final class GuiMachines extends GuiContainer {
    private final ContainerMachine container;
    public GuiMachines(ContainerMachine container) {
        super(container);
        this.container = container;
    }
    @Override public void initGui() {
        super.initGui();
        if (container.tile instanceof TileRfEuConverter)
            buttonList.add(new AePanel.Action(0, guiLeft + 38, guiTop + 49, 100, 20, "", I18n.format("ae2expert.gui.switch")));
        else if(RecipeViewer.available())
            buttonList.add(new AePanel.Action(1,guiLeft+110,guiTop+62,58,18,"",I18n.format("ae2expert.gui.recipe")));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==1)RecipeViewer.open();
        else mc.playerController.sendEnchantPacket(container.windowId, button.id);
    }
    @Override public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);
        renderHoveredToolTip(x, y);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks, int mouseX, int mouseY) {
        AePanel.frame(guiLeft, guiTop, xSize, ySize);
        AePanel.slots(guiLeft, guiTop, inventorySlots.inventorySlots);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        boolean converter = container.tile instanceof TileRfEuConverter;
        fontRenderer.drawString(I18n.format("tile.ae2expert." + (converter ? "rf_eu_converter" : "fluix_chamber") + ".name"), 8, 6, 0x252525);
        fontRenderer.drawString(container.energy + " RF" + (converter ? " / " + container.eu + " EU" : " / 20000 RF"), 8, 20, 0x303030);
        if (converter) {
            fontRenderer.drawString(container.rfToEu ? "RF -> EU" : "EU -> RF", 60, 35, 0x305080);
        } else {
            fontRenderer.drawString(I18n.format("ae2expert.gui.progress", container.progress), 8, 66, 0x303030);
        }
    }
}

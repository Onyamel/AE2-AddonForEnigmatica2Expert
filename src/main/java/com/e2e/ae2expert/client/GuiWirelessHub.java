package com.e2e.ae2expert.client;

import com.e2e.ae2expert.wireless.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import java.io.IOException;
import java.util.UUID;

final class GuiWirelessHub extends GuiContainer {
    private static final int PAGE = 8;
    private final WirelessContainer hub;
    private GuiTextField name;
    private UUID selected;
    private int page;
    private UUID awaitingRemoval;

    GuiWirelessHub(WirelessContainer hub) {
        super(hub);
        this.hub = hub;
        xSize = 310;
        ySize = 222;
    }

    @Override public void initGui() {
        super.initGui();
        name = new GuiTextField(0, fontRenderer, guiLeft + 8, guiTop + 172, 156, 18);
        name.setMaxStringLength(40);
        name.setCanLoseFocus(true);
        String[] keys = {"previous", "next", "save", "toggle", "remove"};
        int[] xs = {8, 35, 168, 8, 151};
        int[] ws = {24, 24, 64, 140, 151};
        for (int i = 0; i < keys.length; i++)
            buttonList.add(new GuiButton(i, guiLeft + xs[i], guiTop + (i < 2 ? 145 : i == 2 ? 171 : 195),
                    ws[i], 20, i < 2 ? (i == 0 ? "<" : ">") : I18n.format("ae2expert.radio." + keys[i])));
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) page = Math.max(0, page - 1);
        else if (button.id == 1) page = Math.min(lastPage(), page + 1);
        else {
            WirelessTile.Link link = selectedLink();
            if (link == null) return;
            if (button.id == 3) WirelessOverlay.toggle(hub.position(), link);
            else if (button.id == 4 && !link.identity.equals(awaitingRemoval)) awaitingRemoval = link.identity;
            else {
                WirelessPackets.CHANNEL.sendToServer(new WirelessPackets.Action(hub.windowId, link, button.id == 4, name.getText()));
                awaitingRemoval = null;
            }
        }
    }

    private int lastPage() { return Math.max(0, (hub.entries.size() - 1) / PAGE); }
    private WirelessTile.Link selectedLink() {
        for (WirelessTile.Link link : hub.entries) if (link.identity.equals(selected)) return link;
        return null;
    }

    @Override public void drawScreen(int x, int y, float ticks) {
        page = Math.min(page, lastPage());
        WirelessTile.Link link = selectedLink();
        buttonList.get(0).enabled = page > 0;
        buttonList.get(1).enabled = page < lastPage();
        for (int i = 2; i < 5; i++) buttonList.get(i).enabled = link != null;
        buttonList.get(3).displayString = I18n.format("ae2expert.radio."
                + (link != null && WirelessOverlay.shown(hub.position(), link.identity) ? "hide" : "toggle"));
        buttonList.get(4).displayString = I18n.format("ae2expert.radio."
                + (link != null && link.identity.equals(awaitingRemoval) ? "confirm_remove" : "remove"));
        drawDefaultBackground();
        super.drawScreen(x, y, ticks);
        name.drawTextBox();
    }

    @Override protected void drawGuiContainerBackgroundLayer(float ticks, int x, int y) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xff222b32);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xffc7cbd0);
        for (int i = 0; i < PAGE && page * PAGE + i < hub.entries.size(); i++) {
            WirelessTile.Link link = hub.entries.get(page * PAGE + i);
            int top = guiTop + 23 + i * 15;
            drawRect(guiLeft + 8, top, guiLeft + 302, top + 14, link.identity.equals(selected) ? 0xff85c2c3 : 0xffadb6bf);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRenderer.drawString(I18n.format("ae2expert.radio.count", hub.entries.size()), 8, 8, 0x26343a);
        for (int i = 0; i < PAGE && page * PAGE + i < hub.entries.size(); i++) {
            WirelessTile.Link link = hub.entries.get(page * PAGE + i);
            int yRow = 26 + i * 15;
            String label = link.name.isEmpty() ? link.pos.getX() + ", " + link.pos.getY() + ", " + link.pos.getZ() : link.name;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(label, 172), 15, yRow, 0x172831);
            fontRenderer.drawString(Integer.toString(link.channels), 191, yRow, 0x26343a);
            fontRenderer.drawString(I18n.format(link.online ? "ae2expert.radio.online" : "ae2expert.radio.offline"), 220, yRow,
                    link.online ? 0x004f3a : 0x713931);
            if (WirelessOverlay.shown(hub.position(), link.identity)) fontRenderer.drawString("+", 289, yRow, 0x00645e);
        }
        fontRenderer.drawString((page + 1) + " / " + (lastPage() + 1), 66, 151, 0x26343a);
    }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        int localY = y - guiTop - 23;
        if (button == 0 && x >= guiLeft + 8 && x < guiLeft + 302 && localY >= 0 && localY < PAGE * 15) {
            int index = page * PAGE + localY / 15;
            if (index < hub.entries.size()) {
                WirelessTile.Link link = hub.entries.get(index);
                selected = link.identity;
                awaitingRemoval = null;
                name.setText(link.name);
            }
        }
        name.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override protected void keyTyped(char c, int key) throws IOException {
        if (!name.textboxKeyTyped(c, key)) super.keyTyped(c, key);
    }
}

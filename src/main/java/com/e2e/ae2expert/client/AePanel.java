package com.e2e.ae2expert.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Slot;

/** Pixel-aligned AE2-style surfaces shared by the addon's inventory screens. */
final class AePanel extends Gui {
    static final int TEXT = 0x34343a;

    private AePanel() { }

    static void frame(int x, int y, int width, int height) {
        drawRect(x + 1, y, x + width - 1, y + height, 0xff141418);
        drawRect(x, y + 1, x + width, y + height - 1, 0xff141418);
        bevel(x + 1, y + 1, width - 2, height - 2, 0xffc6c6c6, 0xfff6f6f6, 0xff54545a);
    }

    static void divider(int x, int y, int width) {
        drawRect(x, y, x + width, y + 1, 0xff99979e);
        drawRect(x, y + 1, x + width, y + 2, 0xffe5e4e7);
    }

    static void inset(int x, int y, int width, int height) {
        bevel(x, y, width, height, 0xffb3b2b8, 0xff87858f, 0xffe3e2e6);
    }

    static void slot(int x, int y, boolean template) {
        bevel(x - 1, y - 1, 18, 18, template ? 0xff92909d : 0xff8b8b8b,
                template ? 0xff51505d : 0xff373737, 0xffefeff1);
    }

    static void slots(int left, int top, Iterable<Slot> slots) {
        for (Slot slot : slots) {
            if (slot.isEnabled()) slot(left + slot.xPos, top + slot.yPos, false);
        }
    }

    private static void bevel(int x, int y, int width, int height, int fill, int top, int bottom) {
        drawRect(x, y, x + width, y + height, bottom);
        drawRect(x, y, x + width - 1, y + height - 1, top);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    static final class Action extends GuiButton {
        private final String symbol;
        boolean selected;

        Action(int id, int x, int y, int width, int height, String symbol, String label) {
            super(id, x, y, width, height, label);
            this.symbol = symbol;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            int fill = selected ? 0xffada0ba : hovered && enabled ? 0xffe1dbe8 : 0xffbcbac2;
            drawRect(x, y, x + width, y + height, 0xff4e4a55);
            bevel(x + 1, y + 1, width - 2, height - 2, fill, 0xfff3f1f5, 0xff77727f);
            int color = enabled ? TEXT : 0x74717a;
            int labelY = y + (height - 8) / 2;
            if (symbol.isEmpty()) {
                mc.fontRenderer.drawString(displayString, x + (width - mc.fontRenderer.getStringWidth(displayString)) / 2,
                        labelY, color);
            } else {
                mc.fontRenderer.drawString(symbol, x + 5, labelY, color);
                mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(displayString, width - 20), x + 16, labelY, color);
            }
        }
    }
}

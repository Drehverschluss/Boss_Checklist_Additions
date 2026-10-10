package com.drehverschluss.bosschecklistadditions.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class NoServerDataSection implements PanelSection {
    private static final Component TEXT = Component.translatable("gui.boss_checklist_additions.no_server_data");

    @Override
    public int height(Font font, int width) {
        return font.wordWrapHeight(TEXT, width);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y, int width, Hover hover) {
        graphics.drawWordWrap(font, TEXT, x, y, width, PackInfoPanel.COLOR_TEXT);
    }
}
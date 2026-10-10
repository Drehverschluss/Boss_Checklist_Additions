package com.drehverschluss.bosschecklistadditions.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** One block of the Pack Info panel (e.g. strength). Sections are stacked from top to bottom. */
public interface PanelSection {
    int height(Font font, int width);

    void render(GuiGraphics graphics, Font font, int x, int y, int width, Hover hover);
}
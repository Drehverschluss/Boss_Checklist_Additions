package com.drehverschluss.bosschecklistadditions.client.panel;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossStats;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The panel on the right book page. Built as a list of sections, so more sections can be added later.
 * Content that is taller than the page can be scrolled with the mouse wheel.
 */
public final class PackInfoPanel {
    public static final int COLOR_TEXT = 0xFF3F3F3F;
    public static final int COLOR_MUTED = 0xFF616161;

    private static final int SECTION_GAP = 4;
    private static final int SCROLL_STEP = 12;
    private static final int SCROLLBAR_COLOR = 0xFF8A6D46;
    private static final int SCROLLBAR_TRACK_COLOR = 0x33000000;

    private static int scroll;
    private static int maxScroll;

    private PackInfoPanel() {
    }

    public static List<PanelSection> sections(@Nullable BossStats stats) {
        if (stats == null) {
            return List.of(new NoServerDataSection());
        }
        List<PanelSection> sections = new ArrayList<>();
        sections.add(new StrengthSection(stats));
        PanelSection location = LocationSection.of(stats);
        if (location != null) {
            sections.add(location);
        }
        return sections;
    }

    public static void resetScroll() {
        scroll = 0;
    }

    /** Scrolls by mouse wheel steps (positive = up). Returns true if there is something to scroll. */
    public static boolean scrollBy(double wheel) {
        if (maxScroll <= 0) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(wheel) * SCROLL_STEP));
        return true;
    }

    /** Draws the panel. Returns the tooltip for the text under the mouse, if any. */
    @Nullable
    public static Component render(GuiGraphics graphics, Font font, int x, int y, int width, int height, List<PanelSection> sections,
            int mouseX, int mouseY) {
        int total = -SECTION_GAP;
        for (PanelSection section : sections) {
            total += section.height(font, width) + SECTION_GAP;
        }
        maxScroll = Math.max(0, total - height);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        Hover hover = new Hover(mouseX, mouseY, x, y, x + width, y + height);
        graphics.enableScissor(x, y, x + width, y + height);
        int cursor = y - scroll;
        for (PanelSection section : sections) {
            section.render(graphics, font, x, cursor, width, hover);
            cursor += section.height(font, width) + SECTION_GAP;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int barX = x + width + 3;
            int thumb = Math.max(8, height * height / total);
            int thumbY = y + (height - thumb) * scroll / maxScroll;
            graphics.fill(barX, y, barX + 2, y + height, SCROLLBAR_TRACK_COLOR);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumb, SCROLLBAR_COLOR);
        }
        return hover.tooltip();
    }
}
package com.drehverschluss.bosschecklistadditions.client;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.client.panel.PackInfoPanel;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.Settings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Geometry and drawing of the Pack Info tab. All positions are relative to the top left corner of the 512x256 book,
 * like the ones used by Boss Checklist. The tab is a bookmark next to the wiki bookmark at the bottom of the right page.
 */
public final class PackInfoTab {
    private static final ResourceLocation BOOKMARK = Constants.id("textures/gui/bookmark_pack_info.png");
    private static final Component TITLE = Component.translatable("gui.boss_checklist_additions.pack_info");

    private static final int BOOKMARK_X = 323;
    private static final int BOOKMARK_Y = 210;
    private static final int BOOKMARK_WIDTH = 16;
    private static final int BOOKMARK_HEIGHT = 27;
    private static final int BOOKMARK_PULL_OUT = 3;

    // Same area where Boss Checklist draws the drops and the spawn text.
    private static final int PANEL_X = 265;
    private static final int PANEL_Y = 80;
    private static final int PANEL_WIDTH = 117;
    private static final int PANEL_HEIGHT = 125;

    // The screen that currently shows the panel, so that the mouse wheel event can find out if it is for us.
    @Nullable
    private static Screen activeScreen;

    private PackInfoTab() {
    }

    public static void setActive(@Nullable Screen screen) {
        activeScreen = screen;
    }

    public static void onOpened() {
        PackInfoPanel.resetScroll();
    }

    /** Returns true if the scroll event was used for the panel. */
    public static boolean onScroll(Screen screen, double mouseX, double mouseY, double wheel) {
        if (screen != activeScreen) {
            return false;
        }
        double x = mouseX - (screen.width - 512) / 2 - PANEL_X;
        double y = mouseY - (screen.height - 256) / 2 - PANEL_Y;
        if (x < 0 || x > PANEL_WIDTH + 8 || y < 0 || y > PANEL_HEIGHT) {
            return false;
        }
        return PackInfoPanel.scrollBy(wheel);
    }

    /**
     * Without server data the tab explains why it is empty. If the server sent data but has none for this boss
     * (hidden by its pack file), there is no tab. The showPackInfoTab option turns the tab off.
     */
    public static boolean isAvailable(String bossId, boolean statsVisible) {
        return Settings.showPackInfoTab() && statsVisible && (!ClientBossStatsCache.hasServerData() || ClientBossStatsCache.get(bossId) != null);
    }

    public static boolean isOverBookmark(double mouseX, double mouseY, int left, int top) {
        double x = mouseX - left - BOOKMARK_X;
        double y = mouseY - top - BOOKMARK_Y;
        return x >= 0 && x <= BOOKMARK_WIDTH && y >= 0 && y <= BOOKMARK_HEIGHT;
    }

    public static void renderBookmark(GuiGraphics graphics, Font font, int left, int top, int mouseX, int mouseY, boolean open) {
        int y = top + BOOKMARK_Y + (open ? BOOKMARK_PULL_OUT : 0);
        graphics.blit(BOOKMARK, left + BOOKMARK_X, y, 0.0f, 0.0f, BOOKMARK_WIDTH, BOOKMARK_HEIGHT, BOOKMARK_WIDTH, BOOKMARK_HEIGHT);
        if (isOverBookmark(mouseX, mouseY, left, top)) {
            graphics.renderTooltip(font, TITLE, mouseX, mouseY);
        }
    }

    public static void renderPanel(GuiGraphics graphics, Font font, int left, int top, String bossId, int mouseX, int mouseY) {
        @Nullable BossStats stats = ClientBossStatsCache.get(bossId);
        Component tooltip = PackInfoPanel.render(graphics, font, left + PANEL_X, top + PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT,
                PackInfoPanel.sections(stats), mouseX, mouseY);
        if (tooltip != null) {
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }}

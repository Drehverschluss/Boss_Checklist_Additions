package com.drehverschluss.bosschecklistadditions.client.panel;

import java.util.ArrayList;
import java.util.List;

import com.drehverschluss.bosschecklistadditions.client.ClientBossStatsCache;
import com.drehverschluss.bosschecklistadditions.client.StatFormat;
import com.drehverschluss.bosschecklistadditions.client.TierDisplay;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Settings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

/** Level, health, armor and attack (solo), each with its base value, and a small footer with the details. */
final class StrengthSection implements PanelSection {
    private static final float SMALL_SCALE = 0.75f;
    private static final int COLUMN_GAP = 6;
    private static final int TITLE_GAP = 2;
    private static final int FOOTER_GAP = 2;
    private static final int TIER_GAP = 4;
    private static final String SEPARATOR = " \u00b7 ";

    private record Row(Component label, String value, String base) {
        boolean showBase() {
            return !base.equals(value);
        }

        Component baseText() {
            return Component.translatable("gui.boss_checklist_additions.base", base);
        }
    }

    private final BossStats stats;

    StrengthSection(BossStats stats) {
        this.stats = stats;
    }

    private List<Row> rows() {
        return List.of(
                new Row(Component.translatable("gui.boss_checklist_additions.health"),
                        StatFormat.approximate(stats.approximate(BossStats.APPROX_HEALTH)) + StatFormat.whole(stats.hpMin(), stats.hpMax()),
                        StatFormat.whole(stats.baseHp(), stats.baseHp())),
                new Row(Component.translatable("gui.boss_checklist_additions.armor"),
                        StatFormat.approximate(stats.approximate(BossStats.APPROX_ARMOR)) + StatFormat.decimal(stats.armorMin(), stats.armorMax()),
                        StatFormat.decimal(stats.baseArmor(), stats.baseArmor())),
                new Row(Component.translatable("gui.boss_checklist_additions.attack"),
                        StatFormat.approximate(stats.approximate(BossStats.APPROX_ATTACK)) + StatFormat.decimal(stats.attackMin(), stats.attackMax()),
                        StatFormat.decimal(stats.baseAttack(), stats.baseAttack())));
    }

    private Component levelText() {
        if (!stats.hasLevel()) {
            return Component.translatable("gui.boss_checklist_additions.level_by_location");
        }
        return stats.levelMin() == stats.levelMax()
                ? Component.translatable("gui.boss_checklist_additions.level_fixed", stats.levelMax())
                : Component.literal(StatFormat.level(stats));
    }

    private boolean showTierLine() {
        return Settings.showTier() && stats.tier().isPresent();
    }

    // Bold, in the tier's own color (pack-provided name/color, or the automatic gradient from green to violet).
    private Component tierText() {
        int tier = stats.tier().orElseThrow();
        int color = TierDisplay.color(tier, ClientBossStatsCache.maxTier()) & 0xFFFFFF;
        return TierDisplay.name(tier).copy().withStyle(style -> style.withBold(true).withColor(TextColor.fromRgb(color)));
    }

    // "Solo · Base: Pack · incl. Dungeon Difficulty"
    private Component footerText() {
        List<Component> parts = new ArrayList<>();
        parts.add(Component.translatable("gui.boss_checklist_additions.note.solo"));
        switch (stats.baseSource()) {
            case PACK -> parts.add(Component.translatable("gui.boss_checklist_additions.note.base_pack"));
            case OBSERVED -> parts.add(Component.translatable("gui.boss_checklist_additions.note.base_observed"));
            case ENTITY -> {
            }
        }
        if (stats.dungeonScaled()) {
            parts.add(Component.translatable("gui.boss_checklist_additions.note.dungeon_difficulty"));
        }
        MutableComponent text = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                text.append(SEPARATOR);
            }
            text.append(parts.get(i));
        }
        return text;
    }

    private List<FormattedCharSequence> footerLines(Font font, int width) {
        return font.split(footerText(), (int) (width / SMALL_SCALE));
    }

    private static int smallLine(Font font) {
        return Math.round(font.lineHeight * SMALL_SCALE) + 1;
    }

    private static int smallWidth(Font font, Component text) {
        return (int) (font.width(text) * SMALL_SCALE);
    }

    private static int labelColumn(Font font, List<Row> rows, Component levelLabel) {
        int widest = font.width(levelLabel);
        for (Row row : rows) {
            widest = Math.max(widest, font.width(row.label()));
        }
        return widest + COLUMN_GAP;
    }

    // The small base value is right-aligned on the same line if it fits, otherwise it moves below the value.
    private static boolean baseFitsOnLine(Font font, Row row, int valueX, int width) {
        return valueX + font.width(row.value()) + COLUMN_GAP + smallWidth(font, row.baseText()) <= width;
    }

    @Override
    public int height(Font font, int width) {
        Component levelLabel = Component.translatable("gui.boss_checklist_additions.level");
        List<Row> rows = rows();
        int valueX = labelColumn(font, rows, levelLabel);

        int height = showTierLine() ? font.lineHeight + TIER_GAP : 0;
        height += font.lineHeight + TITLE_GAP;
        height += Math.max(font.lineHeight, font.wordWrapHeight(levelText(), width - valueX)) + 1;
        for (Row row : rows) {
            height += font.lineHeight + 1;
            if (row.showBase() && !baseFitsOnLine(font, row, valueX, width)) {
                height += smallLine(font);
            }
        }
        return height + FOOTER_GAP + footerLines(font, width).size() * smallLine(font);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y, int width, Hover hover) {
        Component levelLabel = Component.translatable("gui.boss_checklist_additions.level");
        List<Row> rows = rows();
        int valueX = x + labelColumn(font, rows, levelLabel);

        if (showTierLine()) {
            graphics.drawString(font, tierText(), x, y, PackInfoPanel.COLOR_TEXT, false);
            y += font.lineHeight + TIER_GAP;
        }

        graphics.drawString(font, Component.translatable("gui.boss_checklist_additions.strength").withStyle(ChatFormatting.BOLD),
                x, y, PackInfoPanel.COLOR_TEXT, false);
        y += font.lineHeight + TITLE_GAP;

        graphics.drawString(font, levelLabel, x, y, PackInfoPanel.COLOR_TEXT, false);
        Component level = levelText();
        graphics.drawWordWrap(font, level, valueX, y, x + width - valueX, PackInfoPanel.COLOR_TEXT);
        y += Math.max(font.lineHeight, font.wordWrapHeight(level, x + width - valueX)) + 1;

        for (Row row : rows) {
            graphics.drawString(font, row.label(), x, y, PackInfoPanel.COLOR_TEXT, false);
            graphics.drawString(font, row.value(), valueX, y, PackInfoPanel.COLOR_TEXT, false);
            int next = y + font.lineHeight + 1;
            if (row.showBase()) {
                Component base = row.baseText();
                if (baseFitsOnLine(font, row, valueX - x, width)) {
                    drawSmall(graphics, font, base.getVisualOrderText(), x + width - smallWidth(font, base), y + 1);
                } else {
                    drawSmall(graphics, font, base.getVisualOrderText(), valueX, next);
                    next += smallLine(font);
                }
            }
            y = next;
        }
        y += FOOTER_GAP;
        for (FormattedCharSequence line : footerLines(font, width)) {
            drawSmall(graphics, font, line, x, y);
            y += smallLine(font);
        }
    }

    private static void drawSmall(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(SMALL_SCALE, SMALL_SCALE, 1.0f);
        graphics.drawString(font, text, 0, 0, PackInfoPanel.COLOR_MUTED, false);
        graphics.pose().popPose();
    }
}

package com.drehverschluss.bosschecklistadditions.client.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.drehverschluss.bosschecklistadditions.client.NameLookup;
import com.drehverschluss.bosschecklistadditions.core.BossLocation;
import com.drehverschluss.bosschecklistadditions.core.BossStats;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Two lines: "dimension · place" and "zone · loot". Missing parts are left out. A line that is too wide is cut with
 * an ellipsis, and its full text is shown as a tooltip.
 */
final class LocationSection implements PanelSection {
    private static final int TITLE_GAP = 2;
    private static final String SEPARATOR = " \u00b7 ";

    private final List<Component> lines;

    private LocationSection(List<Component> lines) {
        this.lines = lines;
    }

    /** Returns null if there is nothing to show. */
    static LocationSection of(BossStats stats) {
        List<Component> lines = new ArrayList<>();

        List<Component> where = new ArrayList<>();
        stats.location().ifPresent(location -> {
            location.dimension().map(NameLookup::dimension).ifPresent(where::add);
            place(location).ifPresent(where::add);
        });
        if (!where.isEmpty()) {
            lines.add(join(where));
        }

        List<Component> zone = new ArrayList<>();
        stats.zone().ifPresent(value -> {
            zone.add(NameLookup.zone(value));
            if (value.allowLoot()) {
                zone.add(Component.translatable("gui.boss_checklist_additions.loot", value.rewardLevel()));
            }
        });
        if (!zone.isEmpty()) {
            lines.add(join(zone));
        }
        return lines.isEmpty() ? null : new LocationSection(lines);
    }

    private static Component join(List<Component> parts) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result.append(SEPARATOR);
            }
            result.append(parts.get(i));
        }
        return result;
    }

    private static Optional<Component> place(BossLocation location) {
        return location.type().flatMap(type -> switch (type) {
            case STRUCTURE -> location.id().map(NameLookup::structure);
            case BIOME -> location.id().map(NameLookup::biome);
            case SUMMONED -> Optional.of(Component.translatable("gui.boss_checklist_additions.place.summoned"));
            case NATURAL -> Optional.of(Component.translatable("gui.boss_checklist_additions.place.natural"));
        });
    }

    @Override
    public int height(Font font, int width) {
        return font.lineHeight + TITLE_GAP + lines.size() * (font.lineHeight + 1);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y, int width, Hover hover) {
        graphics.drawString(font, Component.translatable("gui.boss_checklist_additions.location").withStyle(ChatFormatting.BOLD),
                x, y, PackInfoPanel.COLOR_TEXT, false);
        y += font.lineHeight + TITLE_GAP;
        for (Component line : lines) {
            TextFit.Fitted fitted = TextFit.fit(font, line, width);
            graphics.drawString(font, fitted.text(), x, y, PackInfoPanel.COLOR_TEXT, false);
            if (fitted.shortened()) {
                hover.offer(x, y, width, font.lineHeight, line);
            }
            y += font.lineHeight + 1;
        }
    }
}
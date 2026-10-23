package com.drehverschluss.bosschecklistadditions.client;

import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Settings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Builds the lines of the health tooltip. Has no client-only Minecraft class dependencies. */
public final class StatsText {
    private static final String HEALTH_LABEL = "gui.boss_checklist.health";
    private static final String ARMOR_LABEL = "gui.boss_checklist.armor";

    private StatsText() {
    }

    public static Component health(BossStats stats) {
        return line(HEALTH_LABEL, stats.approximate(BossStats.APPROX_HEALTH),
                StatFormat.whole(stats.hpMin(), stats.hpMax()), StatFormat.whole(stats.baseHp(), stats.baseHp()));
    }

    public static Component armor(BossStats stats) {
        return line(ARMOR_LABEL, stats.approximate(BossStats.APPROX_ARMOR),
                StatFormat.decimal(stats.armorMin(), stats.armorMax()), StatFormat.decimal(stats.baseArmor(), stats.baseArmor()));
    }

    /**
     * "Lvl 120 · Solo", "Solo", each with " · Dungeon 3" if a Dungeon Difficulty zone changes the boss, and
     * " · Tier 7" (in the tier's own color) if the pack gives this boss a tier and showTier is on.
     */
    public static Component levelAndSolo(BossStats stats) {
        MutableComponent text = stats.hasLevel()
                ? Component.translatable("tooltip.boss_checklist_additions.level_solo", StatFormat.level(stats))
                : Component.translatable("tooltip.boss_checklist_additions.solo");
        stats.zone().filter(zone -> zone.scalesEntity()).ifPresent(zone -> text.append(" \u00b7 ").append(NameLookup.zone(zone)));
        if (Settings.showTier()) {
            stats.tier().ifPresent(tier -> TierDisplay.appendTier(text, tier, ClientBossStatsCache.maxTier()));
        }
        return text.withStyle(ChatFormatting.GRAY);
    }

    // The label of Boss Checklist already contains its color code, so it is kept as it is.
    // The base value is left out if it equals the shown value.
    private static Component line(String labelKey, boolean approximate, String value, String base) {
        MutableComponent line = Component.literal(Component.translatable(labelKey).getString() + StatFormat.approximate(approximate) + value);
        if (!value.equals(base)) {
            line.append(Component.literal(" ").append(Component.translatable("tooltip.boss_checklist_additions.base", base))
                    .withStyle(ChatFormatting.GRAY));
        }
        return line;
    }
}
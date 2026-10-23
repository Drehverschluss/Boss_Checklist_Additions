package com.drehverschluss.bosschecklistadditions.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * Name and color of a pack-defined boss tier. The addon has no idea what a pack's tiers mean, only that they are
 * whole numbers from 1 to the pack's own highest tier (maxTier): the name and color are either taken from
 * language keys the pack provides, or built generically from the tier number alone.
 */
public final class TierDisplay {
    // Fixed color for the highest tier, so the gradient below never has to produce it by interpolation.
    private static final int TOP_TIER_COLOR = 0xFFB04CFF;
    // Darker and more saturated than a neon HSV color, so it stays readable on Boss Checklist's light paper background.
    private static final float SATURATION = 0.80f;
    private static final float BRIGHTNESS = 0.65f;
    private static final float GREEN_HUE = 120.0f;

    private TierDisplay() {
    }

    /** "Tier 7", or whatever a pack's "boss_checklist_additions.tier.&lt;n&gt;" language key says instead. */
    public static Component name(int tier) {
        String custom = "boss_checklist_additions.tier." + tier;
        if (I18n.exists(custom)) {
            return Component.translatable(custom);
        }
        return Component.translatable("gui.boss_checklist_additions.tier", tier);
    }

    /** ARGB color (opaque) for this tier, 1..maxTier. A pack can override it with "boss_checklist_additions.tier.&lt;n&gt;.color". */
    public static int color(int tier, int maxTier) {
        String customColor = "boss_checklist_additions.tier." + tier + ".color";
        if (I18n.exists(customColor)) {
            Integer parsed = parseHexColor(I18n.get(customColor));
            if (parsed != null) {
                return parsed;
            }
        }
        return autoColor(tier, maxTier);
    }

    /**
     * Appends " · Tier 7" (in the tier's own color) to a text, mutating and returning it. Shared so the stats tooltip,
     * the Pack Info tab and the boss list tooltip all show a tier the exact same way.
     */
    public static MutableComponent appendTier(MutableComponent text, int tier, int maxTier) {
        int color = color(tier, maxTier) & 0xFFFFFF;
        return text.append(" \u00b7 ").append(name(tier).copy().withStyle(style -> style.withColor(TextColor.fromRgb(color))));
    }

    // Lowest tier green, highest tier a fixed violet, everything in between a hue gradient from green to red.
    private static int autoColor(int tier, int maxTier) {
        if (maxTier <= 1) {
            return hsvToRgb(GREEN_HUE, SATURATION, BRIGHTNESS);
        }
        if (tier >= maxTier) {
            return TOP_TIER_COLOR;
        }
        int stepCount = maxTier - 1;
        float progress = stepCount <= 1 ? 0.0f : (float) (Math.max(1, tier) - 1) / (stepCount - 1);
        float hue = GREEN_HUE - Math.min(1.0f, Math.max(0.0f, progress)) * GREEN_HUE;
        return hsvToRgb(hue, SATURATION, BRIGHTNESS);
    }

    private static Integer parseHexColor(String text) {
        String cleaned = text.startsWith("#") ? text.substring(1) : text;
        if (cleaned.length() != 6) {
            return null;
        }
        try {
            return 0xFF000000 | Integer.parseInt(cleaned, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        int rgb = java.awt.Color.HSBtoRGB(hue / 360.0f, saturation, value);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}

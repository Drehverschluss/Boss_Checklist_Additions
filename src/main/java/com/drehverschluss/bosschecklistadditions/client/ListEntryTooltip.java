package com.drehverschluss.bosschecklistadditions.client;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Tooltip shown when hovering a boss name in Boss Checklist's own boss list (CustomCheckboxMixin): the full name,
 * if Boss Checklist shortened it for display, with the tier (if any, and showTier is on) appended in its own color.
 * No tooltip at all if the name was not shortened and there is no tier to show.
 */
public final class ListEntryTooltip {
    private ListEntryTooltip() {
    }

    @Nullable
    public static Component build(String shownName, String fullName, Optional<Integer> tier, int maxTier) {
        boolean shortened = !shownName.equals(fullName);
        if (!shortened && tier.isEmpty()) {
            return null;
        }
        MutableComponent text = Component.literal(fullName);
        tier.ifPresent(value -> TierDisplay.appendTier(text, value, maxTier));
        return text;
    }
}

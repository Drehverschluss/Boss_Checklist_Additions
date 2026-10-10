package com.drehverschluss.bosschecklistadditions.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Shortens a text with an ellipsis if it does not fit into a width. */
final class TextFit {
    private static final Component ELLIPSIS = Component.literal("\u2026");

    record Fitted(FormattedCharSequence text, boolean shortened) {
    }

    private TextFit() {
    }

    static Fitted fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return new Fitted(text.getVisualOrderText(), false);
        }
        int available = Math.max(0, width - font.width(ELLIPSIS));
        FormattedCharSequence start = Language.getInstance().getVisualOrder(font.substrByWidth(text, available));
        return new Fitted(FormattedCharSequence.composite(start, ELLIPSIS.getVisualOrderText()), true);
    }
}
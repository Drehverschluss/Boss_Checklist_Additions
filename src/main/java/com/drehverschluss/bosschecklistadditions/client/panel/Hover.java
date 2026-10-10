package com.drehverschluss.bosschecklistadditions.client.panel;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

/** Collects the tooltip for the text under the mouse, e.g. the full text of a shortened line. */
public final class Hover {
    private final int mouseX;
    private final int mouseY;
    private final int clipLeft;
    private final int clipTop;
    private final int clipRight;
    private final int clipBottom;
    @Nullable
    private Component tooltip;

    Hover(int mouseX, int mouseY, int clipLeft, int clipTop, int clipRight, int clipBottom) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    /** Offers a tooltip for a text area. It is used if the mouse is over the visible part of that area. */
    void offer(int x, int y, int width, int height, Component text) {
        boolean overPanel = mouseX >= clipLeft && mouseX < clipRight && mouseY >= clipTop && mouseY < clipBottom;
        if (overPanel && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            tooltip = text;
        }
    }

    @Nullable
    Component tooltip() {
        return tooltip;
    }
}
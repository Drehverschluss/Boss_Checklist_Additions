package com.drehverschluss.bosschecklistadditions.mixin.client;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.drehverschluss.bosschecklistadditions.client.ClientBossStatsCache;
import com.drehverschluss.bosschecklistadditions.client.ListEntryTooltip;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Settings;
import com.drehverschluss.bosschecklistadditions.mixin.MixinSupport;
import com.yori3o.boss_checklist.common.client.boss.BossEntry;
import com.yori3o.boss_checklist.common.client.data.BossNameCache;
import com.yori3o.boss_checklist.common.client.gui.widget.CustomCheckbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * Hovering a boss name in Boss Checklist's own boss list shows its full name (Boss Checklist shortens long names
 * with "..." for the list, see BossNameCache/TextUtil.truncateText) and, if the pack gives it a tier, the tier.
 * The hover area is the label (name) only, matching Boss Checklist's own "hoverLabel" check in renderWidget, not
 * the checkbox circle, and this never touches click handling.
 */
@Mixin(value = CustomCheckbox.class, remap = false)
public abstract class CustomCheckboxMixin extends AbstractWidget {
    @Shadow
    private BossEntry boss;
    // Pixel width/height of the rendered (possibly already shortened) label, computed once in the constructor.
    @Shadow
    private int labelLen;
    @Shadow
    private int labelHeight;

    protected CustomCheckboxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("TAIL"), require = 0)
    private void bca$renderNameTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            if (this.boss == null || !hoverLabel(mouseX, mouseY)) {
                return;
            }
            String bossId = this.boss.id();
            Component fullName = BossNameCache.CACHE.get(bossId);
            if (fullName == null) {
                return;
            }
            Optional<Integer> tier = Settings.showTier() ? tierOf(bossId) : Optional.empty();
            Component tooltip = ListEntryTooltip.build(this.getMessage().getString(), fullName.getString(), tier, ClientBossStatsCache.maxTier());
            if (tooltip != null) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            }
        } catch (Throwable t) {
            MixinSupport.warnOnce("CustomCheckbox name/tier tooltip", t);
        }
    }

    // Same box/label split CustomCheckbox.renderWidget computes for its own "hoverLabel" (box: 8px, then 4px gap).
    private boolean hoverLabel(int mouseX, int mouseY) {
        int labelStart = this.getX() + 8 + 4;
        return mouseX >= labelStart && mouseY >= this.getY() && mouseX < labelStart + this.labelLen && mouseY < this.getY() + this.labelHeight;
    }

    private static Optional<Integer> tierOf(String bossId) {
        @Nullable BossStats stats = ClientBossStatsCache.get(bossId);
        return stats == null ? Optional.empty() : stats.tier();
    }
}

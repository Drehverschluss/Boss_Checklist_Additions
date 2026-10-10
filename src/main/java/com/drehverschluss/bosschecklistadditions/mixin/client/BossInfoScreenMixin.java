package com.drehverschluss.bosschecklistadditions.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.drehverschluss.bosschecklistadditions.client.BossInfoAccess;
import com.drehverschluss.bosschecklistadditions.client.ClientBossStatsCache;
import com.drehverschluss.bosschecklistadditions.client.PackInfoTab;
import com.drehverschluss.bosschecklistadditions.client.StatsText;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.mixin.MixinSupport;
import com.drehverschluss.bosschecklistadditions.core.Settings;
import com.yori3o.boss_checklist.common.client.boss.BossEntry;
import com.yori3o.boss_checklist.common.client.gui.BossInfoScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Boss Checklist only shows health and armor in a tooltip (tooltip_health), built from bossHealth and bossArmor.
 * Both are set to the pack values, and the tooltip lines are rewritten to show ranges and base values.
 * Also adds the Pack Info tab: a bookmark and a panel on the right page.
 */
@Mixin(value = BossInfoScreen.class, remap = false)
public abstract class BossInfoScreenMixin extends Screen {
    @Shadow
    private int bossHealth;
    @Shadow
    private int bossArmor;
    @Shadow
    @Final
    private BossEntry boss;
    @Shadow
    private boolean showInfo;
    @Shadow
    private List<Component> tooltip_health;
    @Shadow
    private Component descriptionText;

    @Unique
    private boolean bca$packInfoOpen;
    @Unique
    private Component bca$savedDescription;

    protected BossInfoScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "initializeEntityStats", at = @At("TAIL"), require = 0)
    private void bca$replaceStats(CallbackInfo ci) {
        try {
            BossStats stats = bca$stats();
            if (stats != null) {
                bossHealth = (int) Math.round(stats.hpMax());
                bossArmor = (int) Math.round(stats.armorMax());
            }
        } catch (Throwable t) {
            MixinSupport.warnOnce("BossInfoScreen.initializeEntityStats", t);
        }
    }

    @Inject(method = "initializeHealthTooltip", at = @At("TAIL"), require = 0)
    private void bca$extendHealthTooltip(CallbackInfo ci) {
        try {
            BossStats stats = bca$stats();
            // Without the health and armor lines (disabled, or hidden until defeated) nothing is changed or revealed.
            if (stats == null || !showInfo || tooltip_health.size() < 2) {
                return;
            }
            tooltip_health.set(0, StatsText.health(stats));
            tooltip_health.set(1, StatsText.armor(stats));
            tooltip_health.add(2, StatsText.levelAndSolo(stats));
        } catch (Throwable t) {
            MixinSupport.warnOnce("BossInfoScreen.initializeHealthTooltip", t);
        }
    }

    // The Drop and Spawn buttons only set currentTab. Selecting one of them closes the Pack Info tab.
    @Inject(method = "lambda$createButtons$0", at = @At("HEAD"), require = 0)
    private void bca$closePackInfoOnDrop(CallbackInfo ci) {
        bca$packInfoOpen = false;
    }

    @Inject(method = "lambda$createButtons$2", at = @At("HEAD"), require = 0)
    private void bca$closePackInfoOnSpawn(CallbackInfo ci) {
        bca$packInfoOpen = false;
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void bca$prepareRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            bca$restoreDescription();
            if (!bca$packInfoOpen) {
                PackInfoTab.setActive(null);
                return;
            }
            // Also closes the tab if another tab was selected in a way the lambda injections missed, or if the data disappeared.
            if (!PackInfoTab.isAvailable(boss.id(), showInfo) || !BossInfoAccess.isTabNone((BossInfoScreen) (Object) this)) {
                bca$packInfoOpen = false;
                return;
            }
            // Boss Checklist draws the description on the same spot when no tab is selected.
            bca$savedDescription = descriptionText;
            descriptionText = Component.empty();
        } catch (Throwable t) {
            bca$packInfoOpen = false;
            MixinSupport.warnOnce("BossInfoScreen.render (Pack Info)", t);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void bca$renderPackInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            bca$restoreDescription();
            if (!PackInfoTab.isAvailable(boss.id(), showInfo)) {
                return;
            }
            int left = (width - 512) / 2;
            int top = (height - 256) / 2;
            PackInfoTab.renderBookmark(graphics, font, left, top, mouseX, mouseY, bca$packInfoOpen);
            PackInfoTab.setActive(bca$packInfoOpen ? this : null);
            if (bca$packInfoOpen) {
                PackInfoTab.renderPanel(graphics, font, left, top, boss.id(), mouseX, mouseY);
            }
        } catch (Throwable t) {
            bca$packInfoOpen = false;
            MixinSupport.warnOnce("BossInfoScreen.render (Pack Info)", t);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void bca$clickPackInfo(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (button != 0 || !PackInfoTab.isAvailable(boss.id(), showInfo)) {
                return;
            }
            if (!PackInfoTab.isOverBookmark(mouseX, mouseY, (width - 512) / 2, (height - 256) / 2)) {
                return;
            }
            if (bca$packInfoOpen) {
                bca$packInfoOpen = false;
            } else {
                BossInfoAccess.resetTab((BossInfoScreen) (Object) this);
                bca$packInfoOpen = true;
                PackInfoTab.onOpened();
            }
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            cir.setReturnValue(true);
        } catch (Throwable t) {
            bca$packInfoOpen = false;
            MixinSupport.warnOnce("BossInfoScreen.mouseClicked (Pack Info)", t);
        }
    }

    @Unique
    private void bca$restoreDescription() {
        if (bca$savedDescription != null) {
            descriptionText = bca$savedDescription;
            bca$savedDescription = null;
        }
    }

    @Unique
    private BossStats bca$stats() {
        return Settings.replaceStats() ? ClientBossStatsCache.get(boss.id()) : null;
    }
}

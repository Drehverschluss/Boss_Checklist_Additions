package com.drehverschluss.bosschecklistadditions.mixin.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.drehverschluss.bosschecklistadditions.client.BossInfoAccess;
import com.drehverschluss.bosschecklistadditions.client.ClientBossDropsCache;
import com.drehverschluss.bosschecklistadditions.client.ClientBossStatsCache;
import com.drehverschluss.bosschecklistadditions.client.DropsText;
import com.drehverschluss.bosschecklistadditions.client.PackInfoTab;
import com.drehverschluss.bosschecklistadditions.client.StatsText;
import com.drehverschluss.bosschecklistadditions.core.BossDrop;
import com.drehverschluss.bosschecklistadditions.core.BossDrops;
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
    @Shadow
    private List<String> drops;

    @Unique
    private boolean bca$packInfoOpen;
    @Unique
    private Component bca$savedDescription;
    // Boss Checklist's own list, captured once before we ever touch the drops field.
    @Unique
    private List<String> bca$originalDrops;
    // The ClientBossDropsCache version our current drops field was built from, to notice later updates.
    @Unique
    private int bca$dropsVersion = -1;

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

    // Boss Checklist's own drop list is a static, hand-maintained guess, and some mods give drops in code instead
    // of through the loot table (e.g. the Wither's Nether Star), which the simulation can never see. So instead of
    // replacing the list, the simulated drops (if any) come first, followed by every original entry whose item is
    // not already simulated - those get a "?" label if they did not already have one of their own.
    @Inject(method = "initializeDrops", at = @At("TAIL"), require = 0)
    private void bca$replaceDrops(CallbackInfo ci) {
        try {
            if (bca$originalDrops == null) {
                bca$originalDrops = List.copyOf(drops);
            }
            drops = bca$mergedDrops();
            bca$dropsVersion = ClientBossDropsCache.version();
        } catch (Throwable t) {
            MixinSupport.warnOnce("BossInfoScreen.initializeDrops", t);
        }
    }

    // Boss Checklist only fills the drops list once (initializeDrops), a click on the tab does not reload it.
    // Since the simulation keeps trickling in results after the screen was opened, this refreshes the merged
    // list whenever the client cache changed since it was last built, so the tab updates without reopening it.
    @Inject(method = "renderDrops", at = @At("HEAD"), require = 0)
    private void bca$refreshDrops(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, CallbackInfo ci) {
        try {
            if (bca$originalDrops == null) {
                return;
            }
            int version = ClientBossDropsCache.version();
            if (version != bca$dropsVersion) {
                drops = bca$mergedDrops();
                bca$dropsVersion = version;
            }
        } catch (Throwable t) {
            MixinSupport.warnOnce("BossInfoScreen.renderDrops", t);
        }
    }

    @Unique
    private List<String> bca$mergedDrops() {
        if (!Settings.replaceDrops()) {
            return bca$originalDrops;
        }
        BossDrops real = ClientBossDropsCache.get(boss.id());
        if (real == null || real.drops().isEmpty()) {
            return bca$originalDrops;
        }
        List<String> merged = new ArrayList<>(real.drops().size() + bca$originalDrops.size());
        Set<String> usedIds = new HashSet<>();
        for (BossDrop drop : real.drops()) {
            merged.add(drop.itemId() + "#" + DropsText.label(drop));
            usedIds.add(drop.itemId());
        }
        for (String original : bca$originalDrops) {
            int hash = original.indexOf('#');
            String itemId = hash < 0 ? original : original.substring(0, hash);
            if (usedIds.add(itemId)) {
                merged.add(hash < 0 ? original + "#?" : original);
            }
        }
        return merged;
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

package com.drehverschluss.bosschecklistadditions.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.drehverschluss.bosschecklistadditions.core.EntityNamespaces;
import com.drehverschluss.bosschecklistadditions.mixin.MixinSupport;
import com.drehverschluss.bosschecklistadditions.core.Settings;
import com.yori3o.boss_checklist.common.client.data.BossRegistry;
import com.yori3o.boss_checklist.impl.PlatformUtil;

@Mixin(value = BossRegistry.class, remap = false)
public abstract class BossRegistryMixin {
    @Redirect(method = "reload",
            at = @At(value = "INVOKE", target = "Lcom/yori3o/boss_checklist/impl/PlatformUtil;isModLoaded(Ljava/lang/String;)Z"),
            require = 0)
    private static boolean bca$isModLoadedOrHasEntities(String namespace) {
        if (PlatformUtil.isModLoaded(namespace)) {
            return true;
        }
        try {
            return EntityNamespaces.hasEntityType(namespace);
        } catch (Throwable t) {
            MixinSupport.warnOnce("BossRegistry namespace fix", t);
            return false;
        }
    }

    // Runs right before the name cache is rebuilt, so the cache never sees the removed entries.
    @Inject(method = "reload",
            at = @At(value = "INVOKE", target = "Lcom/yori3o/boss_checklist/common/client/data/BossNameCache;rebuild()V"),
            require = 0)
    private static void bca$removeMissingEntities(CallbackInfo ci) {
        try {
            if (Settings.hideMissingEntities()) {
                BossRegistry.DEFINITIONS.values().removeIf(definition -> !EntityNamespaces.entityExists(definition.id()));
            }
        } catch (Throwable t) {
            MixinSupport.warnOnce("BossRegistry hideMissingEntities", t);
        }
    }
}

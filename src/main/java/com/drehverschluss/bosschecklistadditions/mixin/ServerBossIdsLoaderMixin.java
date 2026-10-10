package com.drehverschluss.bosschecklistadditions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.drehverschluss.bosschecklistadditions.core.EntityNamespaces;
import com.yori3o.boss_checklist.common.server.data.ServerBossIdsLoader;
import com.yori3o.boss_checklist.impl.PlatformUtil;

@Mixin(value = ServerBossIdsLoader.class, remap = false)
public abstract class ServerBossIdsLoaderMixin {
    @Redirect(method = "load",
            at = @At(value = "INVOKE", target = "Lcom/yori3o/boss_checklist/impl/PlatformUtil;isModLoaded(Ljava/lang/String;)Z"),
            require = 0)
    private static boolean bca$isModLoadedOrHasEntities(String namespace) {
        if (PlatformUtil.isModLoaded(namespace)) {
            return true;
        }
        try {
            return EntityNamespaces.hasEntityType(namespace);
        } catch (Throwable t) {
            MixinSupport.warnOnce("ServerBossIdsLoader namespace fix", t);
            return false;
        }
    }
}

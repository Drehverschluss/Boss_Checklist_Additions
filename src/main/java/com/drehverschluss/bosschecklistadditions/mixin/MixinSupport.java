package com.drehverschluss.bosschecklistadditions.mixin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.drehverschluss.bosschecklistadditions.core.Constants;

/** Mixins must never take Boss Checklist down; the first failure of each kind is logged once. */
public final class MixinSupport {
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private MixinSupport() {
    }

    public static void warnOnce(String key, Throwable error) {
        if (WARNED.add(key)) {
            Constants.LOGGER.warn("Boss Checklist Additions: '{}' failed, falling back to the original Boss Checklist behavior. "
                    + "Is the installed Boss Checklist version supported?", key, error);
        }
    }
}

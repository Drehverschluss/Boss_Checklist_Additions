package com.drehverschluss.bosschecklistadditions.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side cache of the last simulated boss drops. */
public final class ServerBossDropsCache {
    private static volatile List<BossDrops> drops = List.of();

    private ServerBossDropsCache() {
    }

    public static List<BossDrops> get() {
        return drops;
    }

    public static void set(List<BossDrops> newDrops) {
        drops = List.copyOf(newDrops);
    }

    /** Replaces the entry of a single boss, e.g. once its simulation finishes. */
    public static synchronized void update(BossDrops newDrops) {
        List<BossDrops> copy = new ArrayList<>(drops);
        copy.removeIf(entry -> entry.entityId().equals(newDrops.entityId()));
        copy.add(newDrops);
        copy.sort(Comparator.comparing(BossDrops::entityId));
        drops = List.copyOf(copy);
    }

    public static void clear() {
        drops = List.of();
    }
}

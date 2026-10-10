package com.drehverschluss.bosschecklistadditions.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side cache of the last calculated boss stats. */
public final class ServerBossStatsCache {
    private static volatile List<BossStats> stats = List.of();

    private ServerBossStatsCache() {
    }

    public static List<BossStats> get() {
        return stats;
    }

    public static void set(List<BossStats> newStats) {
        stats = List.copyOf(newStats);
    }

    /** Replaces or removes the entry of a single boss. */
    public static synchronized void update(String entityId, BossStats newStats) {
        List<BossStats> copy = new ArrayList<>(stats);
        copy.removeIf(entry -> entry.entityId().equals(entityId));
        if (newStats != null) {
            copy.add(newStats);
        }
        copy.sort(Comparator.comparing(BossStats::entityId));
        stats = List.copyOf(copy);
    }

    public static void clear() {
        stats = List.of();
    }
}
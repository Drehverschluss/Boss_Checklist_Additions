package com.drehverschluss.bosschecklistadditions.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossStats;

/** Client-side copy of the stats received from the server. Contains no client-only Minecraft classes. */
public final class ClientBossStatsCache {
    private static volatile Map<String, BossStats> stats = Map.of();
    private static volatile boolean serverData;
    private static volatile int maxTier;

    private ClientBossStatsCache() {
    }

    @Nullable
    public static BossStats get(String entityId) {
        return stats.get(entityId);
    }

    /** True once the server has sent its stats, i.e. the server has this mod installed. */
    public static boolean hasServerData() {
        return serverData;
    }

    /** The highest tier used by any boss, as sent by the server. 0 if no pack file sets a tier (or no server data yet). */
    public static int maxTier() {
        return maxTier;
    }

    public static void replaceAll(List<BossStats> entries, int maxTier) {
        Map<String, BossStats> map = new HashMap<>();
        for (BossStats entry : entries) {
            map.put(entry.entityId(), entry);
        }
        stats = Map.copyOf(map);
        serverData = true;
        ClientBossStatsCache.maxTier = maxTier;
    }

    public static void update(List<BossStats> entries) {
        Map<String, BossStats> map = new HashMap<>(stats);
        for (BossStats entry : entries) {
            map.put(entry.entityId(), entry);
        }
        stats = Map.copyOf(map);
    }

    public static void clear() {
        stats = Map.of();
        serverData = false;
        maxTier = 0;
    }
}
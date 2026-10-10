package com.drehverschluss.bosschecklistadditions.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;

/** Client-side copy of the simulated drops received from the server. Contains no client-only Minecraft classes. */
public final class ClientBossDropsCache {
    private static volatile Map<String, BossDrops> drops = Map.of();
    // Bumped on every change, so the boss info screen can tell whether its already-merged drop list is stale
    // without comparing the whole map on every frame.
    private static volatile int version;

    private ClientBossDropsCache() {
    }

    @Nullable
    public static BossDrops get(String entityId) {
        return drops.get(entityId);
    }

    public static int version() {
        return version;
    }

    public static void replaceAll(List<BossDrops> entries) {
        Map<String, BossDrops> map = new HashMap<>();
        for (BossDrops entry : entries) {
            map.put(entry.entityId(), entry);
        }
        drops = Map.copyOf(map);
        version++;
    }

    public static void update(List<BossDrops> entries) {
        Map<String, BossDrops> map = new HashMap<>(drops);
        for (BossDrops entry : entries) {
            map.put(entry.entityId(), entry);
        }
        drops = Map.copyOf(map);
        version++;
    }

    public static void clear() {
        drops = Map.of();
        version++;
    }
}

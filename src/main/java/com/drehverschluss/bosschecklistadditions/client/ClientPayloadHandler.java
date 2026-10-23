package com.drehverschluss.bosschecklistadditions.client;

import com.drehverschluss.bosschecklistadditions.core.network.BossDropsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossDropsUpdatePayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsUpdatePayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(BossStatsPayload payload) {
        ClientBossStatsCache.replaceAll(payload.entries(), payload.maxTier());
    }

    public static void handleUpdate(BossStatsUpdatePayload payload) {
        ClientBossStatsCache.update(payload.entries());
    }

    public static void handle(BossDropsPayload payload) {
        ClientBossDropsCache.replaceAll(payload.entries());
    }

    public static void handleUpdate(BossDropsUpdatePayload payload) {
        ClientBossDropsCache.update(payload.entries());
    }
}
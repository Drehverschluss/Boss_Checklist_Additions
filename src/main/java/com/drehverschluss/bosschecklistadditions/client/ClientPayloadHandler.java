package com.drehverschluss.bosschecklistadditions.client;

import com.drehverschluss.bosschecklistadditions.core.network.BossStatsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsUpdatePayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(BossStatsPayload payload) {
        ClientBossStatsCache.replaceAll(payload.entries());
    }

    public static void handleUpdate(BossStatsUpdatePayload payload) {
        ClientBossStatsCache.update(payload.entries());
    }
}
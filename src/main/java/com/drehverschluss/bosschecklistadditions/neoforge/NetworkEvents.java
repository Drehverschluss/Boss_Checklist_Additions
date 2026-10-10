package com.drehverschluss.bosschecklistadditions.neoforge;

import com.drehverschluss.bosschecklistadditions.client.ClientPayloadHandler;
import com.drehverschluss.bosschecklistadditions.core.network.BossDropsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossDropsUpdatePayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsUpdatePayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkEvents {
    private static final String PROTOCOL_VERSION = "5";

    private NetworkEvents() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // Optional: clients without this mod can still join, they just see the original Boss Checklist values.
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToClient(BossStatsPayload.TYPE, BossStatsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(BossStatsUpdatePayload.TYPE, BossStatsUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleUpdate(payload)));
        registrar.playToClient(BossDropsPayload.TYPE, BossDropsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(BossDropsUpdatePayload.TYPE, BossDropsUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleUpdate(payload)));
    }
}
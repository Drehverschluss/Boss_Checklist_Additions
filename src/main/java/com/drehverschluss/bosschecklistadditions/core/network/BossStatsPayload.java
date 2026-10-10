package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The complete list of boss stats. Replaces everything the client knows. */
public record BossStatsPayload(List<BossStats> entries) implements CustomPacketPayload {
    public static final Type<BossStatsPayload> TYPE = new Type<>(Constants.id("boss_stats"));

    public static final StreamCodec<FriendlyByteBuf, BossStatsPayload> STREAM_CODEC =
            BossStatsListCodec.CODEC.map(BossStatsPayload::new, BossStatsPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
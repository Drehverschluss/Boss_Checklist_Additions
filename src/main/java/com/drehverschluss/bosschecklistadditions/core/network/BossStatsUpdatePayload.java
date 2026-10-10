package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Stats of single bosses (e.g. after an observation). Merged into what the client already knows. */
public record BossStatsUpdatePayload(List<BossStats> entries) implements CustomPacketPayload {
    public static final Type<BossStatsUpdatePayload> TYPE = new Type<>(Constants.id("boss_stats_update"));

    public static final StreamCodec<FriendlyByteBuf, BossStatsUpdatePayload> STREAM_CODEC =
            BossStatsListCodec.CODEC.map(BossStatsUpdatePayload::new, BossStatsUpdatePayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
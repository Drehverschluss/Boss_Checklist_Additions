package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The complete list of boss stats. Replaces everything the client knows. maxTier is the highest tier used by any
 * loaded pack file (0 if none set one), sent once per list since the individual tiers of BossStats are pack-specific numbers
 * that only make sense relative to that range (e.g. for the automatic tier color).
 */
public record BossStatsPayload(List<BossStats> entries, int maxTier) implements CustomPacketPayload {
    public static final Type<BossStatsPayload> TYPE = new Type<>(Constants.id("boss_stats"));

    public static final StreamCodec<FriendlyByteBuf, BossStatsPayload> STREAM_CODEC = StreamCodec.composite(
            BossStatsListCodec.CODEC, BossStatsPayload::entries,
            ByteBufCodecs.VAR_INT, BossStatsPayload::maxTier,
            BossStatsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
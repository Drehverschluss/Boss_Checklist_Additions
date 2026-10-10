package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The complete list of simulated boss drops. Replaces everything the client knows. */
public record BossDropsPayload(List<BossDrops> entries) implements CustomPacketPayload {
    public static final Type<BossDropsPayload> TYPE = new Type<>(Constants.id("boss_drops"));

    public static final StreamCodec<FriendlyByteBuf, BossDropsPayload> STREAM_CODEC =
            BossDropsListCodec.CODEC.map(BossDropsPayload::new, BossDropsPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

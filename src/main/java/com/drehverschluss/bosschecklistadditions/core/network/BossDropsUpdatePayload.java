package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Drops of single bosses, finished since the last update. Merged into what the client already knows. */
public record BossDropsUpdatePayload(List<BossDrops> entries) implements CustomPacketPayload {
    public static final Type<BossDropsUpdatePayload> TYPE = new Type<>(Constants.id("boss_drops_update"));

    public static final StreamCodec<FriendlyByteBuf, BossDropsUpdatePayload> STREAM_CODEC =
            BossDropsListCodec.CODEC.map(BossDropsUpdatePayload::new, BossDropsUpdatePayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

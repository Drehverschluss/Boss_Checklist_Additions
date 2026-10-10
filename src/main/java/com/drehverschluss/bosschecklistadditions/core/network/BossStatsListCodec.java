package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.ArrayList;
import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossStats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

final class BossStatsListCodec {
    private static final int MAX_ENTRIES = 4096;

    static final StreamCodec<FriendlyByteBuf, List<BossStats>> CODEC = StreamCodec.of(
            BossStatsListCodec::write, BossStatsListCodec::read);

    private BossStatsListCodec() {
    }

    private static void write(FriendlyByteBuf buf, List<BossStats> entries) {
        buf.writeVarInt(entries.size());
        for (BossStats entry : entries) {
            entry.write(buf);
        }
    }

    private static List<BossStats> read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many boss stats entries: " + size);
        }
        List<BossStats> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(BossStats.read(buf));
        }
        return List.copyOf(entries);
    }
}
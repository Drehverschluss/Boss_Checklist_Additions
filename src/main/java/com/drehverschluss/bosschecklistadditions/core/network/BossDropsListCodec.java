package com.drehverschluss.bosschecklistadditions.core.network;

import java.util.ArrayList;
import java.util.List;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

final class BossDropsListCodec {
    private static final int MAX_ENTRIES = 4096;

    static final StreamCodec<FriendlyByteBuf, List<BossDrops>> CODEC = StreamCodec.of(
            BossDropsListCodec::write, BossDropsListCodec::read);

    private BossDropsListCodec() {
    }

    private static void write(FriendlyByteBuf buf, List<BossDrops> entries) {
        buf.writeVarInt(entries.size());
        for (BossDrops entry : entries) {
            entry.write(buf);
        }
    }

    private static List<BossDrops> read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many boss drops entries: " + size);
        }
        List<BossDrops> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(BossDrops.read(buf));
        }
        return List.copyOf(entries);
    }
}

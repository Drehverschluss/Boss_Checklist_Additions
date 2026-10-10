package com.drehverschluss.bosschecklistadditions.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

/** The simulated drops of one boss, sorted by descending chance. */
public record BossDrops(String entityId, List<BossDrop> drops) {
    private static final int MAX_ID_LENGTH = 256;
    // LootDropsCalculator already keeps only the most likely drops; this is just a safety net against a huge,
    // unbounded loot table (e.g. one that picks from a reference table with hundreds of possible items).
    private static final int MAX_DROPS = 64;

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(entityId, MAX_ID_LENGTH);
        buf.writeVarInt(drops.size());
        for (BossDrop drop : drops) {
            drop.write(buf);
        }
    }

    public static BossDrops read(FriendlyByteBuf buf) {
        String entityId = buf.readUtf(MAX_ID_LENGTH);
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_DROPS) {
            throw new IllegalArgumentException("Too many drops for " + entityId + ": " + size);
        }
        List<BossDrop> drops = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            drops.add(BossDrop.read(buf));
        }
        return new BossDrops(entityId, List.copyOf(drops));
    }
}

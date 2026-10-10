package com.drehverschluss.bosschecklistadditions.core;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One possible drop of a boss, found by simulating its real loot table.
 * chance is between 0 (exclusive) and 1 (inclusive): the share of simulated kills that dropped the item at least once.
 * minCount/maxCount are the smallest/largest amount seen in a single kill that dropped the item.
 */
public record BossDrop(String itemId, double chance, int minCount, int maxCount) {
    private static final int MAX_ID_LENGTH = 256;

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(itemId, MAX_ID_LENGTH);
        buf.writeDouble(chance);
        buf.writeVarInt(minCount);
        buf.writeVarInt(maxCount);
    }

    public static BossDrop read(FriendlyByteBuf buf) {
        return new BossDrop(buf.readUtf(MAX_ID_LENGTH), buf.readDouble(), buf.readVarInt(), buf.readVarInt());
    }
}

package com.drehverschluss.bosschecklistadditions.core;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The Dungeon Difficulty zone of a boss. typeKey is the translation key of the difficulty type,
 * scalesEntity tells whether the zone changes the attributes of the boss.
 */
public record BossZone(String typeKey, int level, int entityLevel, int rewardLevel, boolean allowLoot, boolean scalesEntity) {
    private static final int MAX_KEY_LENGTH = 256;

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(typeKey, MAX_KEY_LENGTH);
        buf.writeVarInt(level);
        buf.writeVarInt(entityLevel);
        buf.writeVarInt(rewardLevel);
        buf.writeBoolean(allowLoot);
        buf.writeBoolean(scalesEntity);
    }

    public static BossZone read(FriendlyByteBuf buf) {
        return new BossZone(buf.readUtf(MAX_KEY_LENGTH), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }
}
package com.drehverschluss.bosschecklistadditions.core;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Computed solo stats of one boss. A level of -1 means "unknown" (no entity-specific leveling settings),
 * in which case the min and max values equal the base values (plus Dungeon Difficulty, if it applies).
 * approximateMask: bit 0 health, bit 1 armor, bit 2 attack; set if a random modifier is part of that value.
 */
public record BossStats(
        String entityId,
        int levelMin, int levelMax,
        double hpMin, double hpMax,
        double armorMin, double armorMax,
        double attackMin, double attackMax,
        double baseHp, double baseArmor, double baseAttack,
        BaseSource baseSource,
        int approximateMask,
        Optional<BossLocation> location,
        Optional<BossZone> zone,
        Optional<Integer> tier) {

    public static final int APPROX_HEALTH = 1;
    public static final int APPROX_ARMOR = 2;
    public static final int APPROX_ATTACK = 4;

    private static final int MAX_ID_LENGTH = 256;

    public boolean hasLevel() {
        return levelMin >= 0;
    }

    public boolean approximate(int bit) {
        return (approximateMask & bit) != 0;
    }

    /** True if Dungeon Difficulty changed the attributes of this boss. */
    public boolean dungeonScaled() {
        return zone.map(BossZone::scalesEntity).orElse(false);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(entityId, MAX_ID_LENGTH);
        buf.writeInt(levelMin);
        buf.writeInt(levelMax);
        buf.writeDouble(hpMin);
        buf.writeDouble(hpMax);
        buf.writeDouble(armorMin);
        buf.writeDouble(armorMax);
        buf.writeDouble(attackMin);
        buf.writeDouble(attackMax);
        buf.writeDouble(baseHp);
        buf.writeDouble(baseArmor);
        buf.writeDouble(baseAttack);
        buf.writeVarInt(baseSource.ordinal());
        buf.writeVarInt(approximateMask);
        buf.writeOptional(location, (out, value) -> value.write(out));
        buf.writeOptional(zone, (out, value) -> value.write(out));
        buf.writeOptional(tier, FriendlyByteBuf::writeVarInt);
    }

    public static BossStats read(FriendlyByteBuf buf) {
        return new BossStats(
                buf.readUtf(MAX_ID_LENGTH),
                buf.readInt(), buf.readInt(),
                buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                BaseSource.byId(buf.readVarInt()),
                buf.readVarInt(),
                buf.readOptional(BossLocation::read),
                buf.readOptional(BossZone::read),
                buf.readOptional(FriendlyByteBuf::readVarInt));
    }
}
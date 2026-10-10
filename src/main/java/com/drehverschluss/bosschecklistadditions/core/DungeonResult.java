package com.drehverschluss.bosschecklistadditions.core;

/** approximateMask: bit 0 health, bit 1 armor, bit 2 attack (set if a random modifier changed that value). */
public record DungeonResult(BossZone zone, int approximateMask) {
}
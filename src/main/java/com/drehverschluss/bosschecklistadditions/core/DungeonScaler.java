package com.drehverschluss.bosschecklistadditions.core;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Abstraction over Dungeon Difficulty, so that the calculator does not depend on its classes. */
public interface DungeonScaler {
    /**
     * Resolves the zone of a boss from its dimension and location (no world position involved) and applies the
     * attribute modifiers of that zone to the temporary entity. Returns null if there is no zone.
     */
    @Nullable
    DungeonResult applyTo(LivingEntity entity, ServerLevel level, ResourceLocation entityId, ResourceLocation dimension,
            @Nullable BossPackData.Location location);
}
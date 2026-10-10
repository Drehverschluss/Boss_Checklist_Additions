package com.drehverschluss.bosschecklistadditions.core;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Abstraction over a leveling mod, so that the calculator does not depend on its classes. */
public interface LevelScaler {
    /** Inclusive level range {min, max}, or null if the entity type has no own leveling settings. */
    @Nullable
    int[] levelRange(EntityType<?> type, ServerLevel level);

    void applyLevel(LivingEntity entity, int level);
}

package com.drehverschluss.bosschecklistadditions.core.compat;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.LevelScaler;

import dev.muon.dynamic_difficulty.api.LevelingAPI;
import dev.muon.dynamic_difficulty.data.DimensionLevelingSettingsStore;
import dev.muon.dynamic_difficulty.data.EntityLevelingSettingsStore;
import dev.muon.dynamic_difficulty.settings.EntityLevelingSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** All Dynamic Difficulty access lives here. Only load this class if the mod is present. */
public final class DynamicDifficultyScaler implements LevelScaler {
    @Nullable
    @Override
    public int[] levelRange(EntityType<?> type, ServerLevel level) {
        EntityLevelingSettings settings = EntityLevelingSettingsStore.get(type, DimensionLevelingSettingsStore.get(level));
        if (settings == null) {
            return null;
        }
        return new int[] {settings.startingLevel(), settings.maxLevel()};
    }

    @Override
    public void applyLevel(LivingEntity entity, int level) {
        LevelingAPI.setAndUpdateLevel(entity, level);
    }
}

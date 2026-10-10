package com.drehverschluss.bosschecklistadditions.core.compat;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossPackData;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.BossZone;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.DungeonResult;
import com.drehverschluss.bosschecklistadditions.core.DungeonScaler;

import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.Difficulty;
import net.dungeon_difficulty.logic.DifficultyTypes;
import net.dungeon_difficulty.logic.MathHelper;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * All Dungeon Difficulty access lives here. Only load this class if the mod is present.
 * Dungeon Difficulty has no API and needs a world position, so its zone resolution is rebuilt here from the
 * public pieces (config, difficulty types, pattern matching), following PatternMatching.getDifficultyResult
 * and EntityScaling.apply. Randomness is not rolled: random modifiers use their mean and are reported as approximate.
 * A single player gets no per-player bonus in Dungeon Difficulty (it starts at 2 players), so none is applied.
 */
public final class DungeonDifficultyScaler implements DungeonScaler {
    private static volatile boolean disabled;

    private record Candidate(Difficulty difficulty) {
    }

    @Nullable
    @Override
    public DungeonResult applyTo(LivingEntity entity, ServerLevel level, ResourceLocation entityId, ResourceLocation dimension,
            @Nullable BossPackData.Location location) {
        if (disabled) {
            return null;
        }
        try {
            return resolveAndApply(entity, level, entityId, dimension, location);
        } catch (Throwable t) {
            disabled = true;
            Constants.LOGGER.warn("Dungeon Difficulty evaluation failed and is turned off until the game is restarted. "
                    + "Is the installed Dungeon Difficulty version supported?", t);
            return null;
        }
    }

    @Nullable
    private static DungeonResult resolveAndApply(LivingEntity entity, ServerLevel level, ResourceLocation entityId, ResourceLocation dimension,
            @Nullable BossPackData.Location location) {
        Config config = DungeonDifficulty.config.value;
        if (config == null || config.dimensions == null) {
            return null;
        }

        for (Config.Dimension dimensionConfig : config.dimensions) {
            if (dimensionConfig == null || !matchesDimension(dimensionConfig.world_matches, dimension)) {
                continue;
            }
            Candidate chosen = chooseHigher(resolveZone(dimensionConfig, level, location), resolveEntity(dimensionConfig, entityId));
            if (chosen == null) {
                Difficulty wide = findDifficulty(dimensionConfig.difficulty);
                if (wide != null && wide.isValid()) {
                    chosen = new Candidate(wide);
                }
            }
            if (chosen != null) {
                return apply(chosen.difficulty(), entity);
            }
        }
        return null;
    }

    private static boolean matchesDimension(@Nullable Config.Dimension.Filters filters, ResourceLocation dimension) {
        return filters == null || PatternMatching.universalMatch(dimension, filters.dimension);
    }

    // The first zone (in config order) that matches wins, then the first matching zone specifier changes its type.
    @Nullable
    private static Candidate resolveZone(Config.Dimension dimensionConfig, ServerLevel level, @Nullable BossPackData.Location location) {
        if (dimensionConfig.zones == null) {
            return null;
        }
        Candidate result = null;
        for (Config.Zone zone : dimensionConfig.zones) {
            if (zone == null || !matchesZone(zone.zone_matches, level, location)) {
                continue;
            }
            Difficulty difficulty = findDifficulty(zone.difficulty);
            if (difficulty != null && difficulty.isValid()) {
                result = new Candidate(difficulty);
                break;
            }
        }
        if (result == null || dimensionConfig.zone_specifiers == null) {
            return result;
        }
        for (Config.Zone.TypeOverride specifier : dimensionConfig.zone_specifiers) {
            if (specifier != null && matchesZone(specifier.zone_matches, level, location)) {
                Config.DifficultyType type = findType(specifier.difficulty_name);
                return type == null ? result : new Candidate(result.difficulty().withType(type));
            }
        }
        return result;
    }

    // Same rules as LocationData.matches(Zone.Filters), but the biome and structure come from the pack file.
    // A filter that cannot be evaluated (e.g. a structure filter for a boss that lives in a biome) does not match.
    private static boolean matchesZone(@Nullable Config.Zone.Filters filters, ServerLevel level, @Nullable BossPackData.Location location) {
        if (filters == null) {
            return true;
        }
        if (filters.biome != null && !filters.biome.isEmpty()) {
            Optional<? extends Holder<net.minecraft.world.level.biome.Biome>> biome = holder(level, Registries.BIOME, location, BossPackData.LocationType.BIOME);
            if (biome.isEmpty() || !PatternMatching.universalMatch(biome.get(), Registries.BIOME, filters.biome)) {
                return false;
            }
        }
        if (filters.structure != null && !filters.structure.isEmpty()) {
            Optional<? extends Holder<net.minecraft.world.level.levelgen.structure.Structure>> structure =
                    holder(level, Registries.STRUCTURE, location, BossPackData.LocationType.STRUCTURE);
            return structure.isPresent() && PatternMatching.universalMatch(structure.get(), Registries.STRUCTURE, filters.structure);
        }
        return true;
    }

    private static <T> Optional<? extends Holder<T>> holder(ServerLevel level, ResourceKey<Registry<T>> registryKey, @Nullable BossPackData.Location location,
            BossPackData.LocationType expected) {
        if (location == null || location.type() != expected || location.id().isEmpty()) {
            return Optional.empty();
        }
        return level.registryAccess().registryOrThrow(registryKey).getHolder(ResourceKey.create(registryKey, location.id().get()));
    }

    // Only the entity entries matter here, the loot table entries belong to the loot scaling goal.
    @Nullable
    private static Candidate resolveEntity(Config.Dimension dimensionConfig, ResourceLocation entityId) {
        if (dimensionConfig.entities == null) {
            return null;
        }
        Optional<Holder.Reference<net.minecraft.world.entity.EntityType<?>>> holder = BuiltInRegistries.ENTITY_TYPE.getHolder(entityId);
        if (holder.isEmpty()) {
            return null;
        }
        for (Config.EntityMatcher matcher : dimensionConfig.entities) {
            if (matcher == null || matcher.entity_type == null) {
                continue;
            }
            if (PatternMatching.universalMatch(holder.get(), Registries.ENTITY_TYPE, matcher.entity_type)) {
                Difficulty difficulty = findDifficulty(matcher.difficulty);
                if (difficulty != null) {
                    return new Candidate(difficulty);
                }
            }
        }
        return null;
    }

    // Like PatternMatching.chooseHigherDifficulty: the zone wins a tie.
    @Nullable
    private static Candidate chooseHigher(@Nullable Candidate zone, @Nullable Candidate entity) {
        if (zone == null && entity == null) {
            return null;
        }
        int zoneLevel = zone != null ? zone.difficulty().level() : -100;
        int entityLevel = entity != null ? entity.difficulty().level() : -100;
        return zoneLevel >= entityLevel ? zone : entity;
    }

    @Nullable
    private static Difficulty findDifficulty(@Nullable Config.DifficultyReference reference) {
        if (reference == null || reference.name == null || reference.name.isEmpty()) {
            return null;
        }
        Config.DifficultyType type = findType(reference.name);
        if (type == null) {
            return null;
        }
        int rewardLevel = reference.reward_level != null ? reference.reward_level : reference.level;
        int entityLevel = reference.entity_level != null ? reference.entity_level : reference.level;
        return new Difficulty(type, reference.level, entityLevel, rewardLevel);
    }

    @Nullable
    private static Config.DifficultyType findType(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (Config.DifficultyType type : DifficultyTypes.resolved) {
            if (name.equals(type.name)) {
                return type;
            }
        }
        return null;
    }

    private static DungeonResult apply(Difficulty difficulty, LivingEntity entity) {
        int approximateMask = 0;
        boolean scales = false;
        int level = difficulty.entityLevel();
        Config.DifficultyType type = difficulty.type();

        if (level > 0 && type != null && type.entities != null) {
            PatternMatching.EntityData data = PatternMatching.EntityData.create(entity);
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(DungeonDifficulty.MODID, "location");
            for (Config.EntityModifier modifier : PatternMatching.getModifiersForEntity(type.entities, data)) {
                if (modifier.attributes == null) {
                    continue;
                }
                for (Config.AttributeModifier attributeModifier : modifier.attributes) {
                    int applied = applyModifier(attributeModifier, level, entity, modifierId);
                    if (applied >= 0) {
                        scales = true;
                        if (attributeModifier.randomness > 0) {
                            approximateMask |= applied;
                        }
                    }
                }
            }
        }

        BossZone zone = new BossZone(difficulty.typeTranslationKey(), difficulty.level(), difficulty.entityLevel(),
                difficulty.rewardLevel(), difficulty.allowsLootScaling(), scales);
        return new DungeonResult(zone, approximateMask);
    }

    // Mirrors EntityScaling.apply. Returns -1 if nothing was applied, otherwise the approximate bit of the changed attribute
    // (0 for attributes that are not part of the shown stats).
    private static int applyModifier(Config.AttributeModifier modifier, int level, LivingEntity entity, ResourceLocation modifierId) {
        if (modifier.attribute == null || modifier.attribute.isEmpty()) {
            return -1;
        }
        ResourceLocation attributeId = ResourceLocation.tryParse(modifier.attribute);
        if (attributeId == null) {
            return -1;
        }
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
        if (holder.isEmpty() || !entity.getAttributes().hasAttribute(holder.get())) {
            return -1;
        }

        float amount = modifier.offset + modifier.value * level;
        amount = (float) MathHelper.round(amount, modifier.value * 0.25f);
        AttributeModifier.Operation operation = modifier.operation == Config.Operation.ADDITION
                ? AttributeModifier.Operation.ADD_VALUE
                : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

        AttributeInstance instance = entity.getAttribute(holder.get());
        if (instance == null || instance.hasModifier(modifierId)) {
            return -1;
        }
        instance.addPermanentModifier(new AttributeModifier(modifierId, amount, operation));
        return approximateBit(holder.get());
    }

    private static int approximateBit(Holder<Attribute> attribute) {
        if (attribute.is(Attributes.MAX_HEALTH.unwrapKey().orElseThrow())) {
            return BossStats.APPROX_HEALTH;
        }
        if (attribute.is(Attributes.ARMOR.unwrapKey().orElseThrow())) {
            return BossStats.APPROX_ARMOR;
        }
        if (attribute.is(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow())) {
            return BossStats.APPROX_ATTACK;
        }
        return 0;
    }
}

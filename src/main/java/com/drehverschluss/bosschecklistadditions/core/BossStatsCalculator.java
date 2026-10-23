package com.drehverschluss.bosschecklistadditions.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.yori3o.boss_checklist.common.server.data.ServerBossIdsLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Computes the solo stats of every boss known to Boss Checklist. Temporary entities are never added to the world. */
public final class BossStatsCalculator {
    private BossStatsCalculator() {
    }

    private record Snapshot(double hp, double armor, double attack) {
        static Snapshot of(LivingEntity entity) {
            double attack = entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                    ? entity.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    : 0.0;
            return new Snapshot(
                    entity.getAttributeValue(Attributes.MAX_HEALTH),
                    entity.getAttributeValue(Attributes.ARMOR),
                    attack);
        }
    }

    public static List<BossStats> calculateAll(MinecraftServer server, @Nullable LevelScaler scaler, @Nullable DungeonScaler dungeon) {
        List<String> ids = new ArrayList<>(ServerBossIdsLoader.LOADED_BOSSES);
        ids.sort(String::compareTo);

        List<BossStats> result = new ArrayList<>();
        for (String id : ids) {
            BossStats stats = calculateOne(id, server, scaler, dungeon);
            if (stats != null) {
                result.add(stats);
            }
        }
        return result;
    }

    /** Returns null for hidden bosses, unknown entity types and bosses that failed to calculate. */
    @Nullable
    public static BossStats calculateOne(String id, MinecraftServer server, @Nullable LevelScaler scaler, @Nullable DungeonScaler dungeon) {
        try {
            return calculate(id, server, scaler, dungeon);
        } catch (Throwable t) {
            Constants.LOGGER.warn("Could not calculate boss stats for '{}'", id, t);
            return null;
        }
    }

    @Nullable
    private static BossStats calculate(String id, MinecraftServer server, @Nullable LevelScaler scaler, @Nullable DungeonScaler dungeon) {
        BossPackData pack = BossPackDataManager.get(id);
        if (pack != null && pack.hidden()) {
            return null;
        }
        ResourceLocation entityId = ResourceLocation.tryParse(id);
        if (entityId == null) {
            return null;
        }
        Optional<EntityType<?>> optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
        if (optionalType.isEmpty()) {
            Constants.debug("Skipping boss '{}': no such entity type", id);
            return null;
        }
        EntityType<?> type = optionalType.get();
        ServerLevel level = server.overworld();

        Entity created = type.create(level);
        if (!(created instanceof LivingEntity entity)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }

        try {
            BaseAttributes packValues = pack != null ? pack.baseAttributes().orElse(BaseAttributes.EMPTY) : BaseAttributes.EMPTY;
            BaseAttributes observedValues = ObservedBaseValues.get(server).get(id);
            if (observedValues == null) {
                observedValues = BaseAttributes.EMPTY;
            }

            // The base values must be in place before a level is applied, the leveling mod scales them.
            BaseSource source = BaseSource.ENTITY;
            source = source.max(overrideBase(entity, Attributes.MAX_HEALTH, packValues.maxHealth(), observedValues.maxHealth()));
            source = source.max(overrideBase(entity, Attributes.ARMOR, packValues.armor(), observedValues.armor()));
            source = source.max(overrideBase(entity, Attributes.ATTACK_DAMAGE, packValues.attackDamage(), observedValues.attackDamage()));

            Snapshot base = Snapshot.of(entity);

            int[] range = null;
            if (scaler != null) {
                try {
                    range = scaler.levelRange(type, level);
                    if (range != null) {
                        range = new int[] {Math.max(0, range[0]), Math.max(Math.max(0, range[0]), range[1])};
                        scaler.applyLevel(entity, range[0]);
                    }
                } catch (Throwable t) {
                    Constants.debug("Could not apply levels to '{}', using base stats", id, t);
                    range = null;
                    resetModifiers(entity);
                }
            }

            // Dungeon Difficulty works on top of the leveling mod. Its modifiers stay on the entity for the second snapshot.
            DungeonResult dungeonResult = applyDungeon(dungeon, entity, level, entityId, pack);
            Snapshot low = Snapshot.of(entity);
            Snapshot high = low;
            int levelMin = -1;
            int levelMax = -1;

            if (range != null) {
                levelMin = range[0];
                levelMax = range[1];
                if (range[1] != range[0]) {
                    try {
                        scaler.applyLevel(entity, range[1]);
                        high = Snapshot.of(entity);
                    } catch (Throwable t) {
                        Constants.debug("Could not apply the maximum level to '{}'", id, t);
                        levelMax = levelMin;
                    }
                }
            }

            Optional<BossLocation> bossLocation = pack == null ? Optional.empty()
                    : pack.dimension().isPresent() || pack.location().isPresent()
                            ? Optional.of(new BossLocation(pack.dimension(), pack.location().map(BossPackData.Location::type), pack.location().flatMap(BossPackData.Location::id)))
                            : Optional.empty();

            return new BossStats(id, levelMin, levelMax,
                    Math.min(low.hp(), high.hp()), Math.max(low.hp(), high.hp()),
                    Math.min(low.armor(), high.armor()), Math.max(low.armor(), high.armor()),
                    Math.min(low.attack(), high.attack()), Math.max(low.attack(), high.attack()),
                    base.hp(), base.armor(), base.attack(), source,
                    dungeonResult != null ? dungeonResult.approximateMask() : 0,
                    bossLocation,
                    Optional.ofNullable(dungeonResult).map(DungeonResult::zone),
                    pack == null ? Optional.empty() : pack.tier());
        } finally {
            entity.discard();
        }
    }

    @Nullable
    private static DungeonResult applyDungeon(@Nullable DungeonScaler dungeon, LivingEntity entity, ServerLevel level, ResourceLocation entityId, @Nullable BossPackData pack) {
        if (dungeon == null || pack == null || pack.dimension().isEmpty()) {
            return null;
        }
        return dungeon.applyTo(entity, level, entityId, pack.dimension().get(), pack.location().orElse(null));
    }

    // Leftovers of a failed level application must not end up in the stats.
    private static void resetModifiers(LivingEntity entity) {
        for (Holder<Attribute> attribute : List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE)) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifiers();
            }
        }
    }

    /** Applies the pack value, else the observed value. Returns the source that was used. */
    private static BaseSource overrideBase(LivingEntity entity, Holder<Attribute> attribute, Optional<Double> pack, Optional<Double> observed) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return BaseSource.ENTITY;
        }
        if (pack.isPresent()) {
            instance.setBaseValue(pack.get());
            return BaseSource.PACK;
        }
        if (observed.isPresent()) {
            instance.setBaseValue(observed.get());
            return BaseSource.OBSERVED;
        }
        return BaseSource.ENTITY;
    }
}
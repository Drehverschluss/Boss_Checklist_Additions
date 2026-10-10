package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossDrop;
import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.BossPackData;
import com.drehverschluss.bosschecklistadditions.core.BossPackDataManager;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.LevelScaler;
import com.drehverschluss.bosschecklistadditions.core.Settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Finds the real drops of a boss by running its actual loot table(s) many times (like a kill by a player without
 * Looting) and counting how often each item appears. Temporary entities are never added to the world and no
 * items are ever spawned. Approximate, not an exact probability: conditions that need information only known
 * at an actual kill (e.g. the exact damage type) use a reasonable stand-in (a plain player attack). If Dynamic
 * Difficulty gives the boss a level range, the trials are spread across it, so level-gated drops show up too.
 *
 * <p>By default the entity's own loot table (entity.getLootTable()) is simulated. A pack file can instead list
 * explicit loot tables (e.g. a reward chest a boss fills on death instead of dropping items itself); all of them
 * are rolled and summed together each trial, each with its own loot params restricted to what its own parameter
 * set actually allows.
 *
 * <p>Runs between {@value #MIN_TRIALS} and {@value #MAX_TRIALS} trials, stopping early once the result has
 * stopped changing (see {@link Session}), and skips the trials entirely if {@link DropsSavedData} already has a
 * result for the same fingerprint (see {@link DropsFingerprint}). One trial runs at a time, so that
 * {@link DropsScheduler} can spread the work, however slow a single loot table turns out to be, across many
 * server ticks and never block the server for longer than a single trial. Needs NeoForge's FakePlayer, so it
 * lives here instead of core.
 */
public final class LootDropsCalculator {
    static final int MIN_TRIALS = 100;
    static final int MAX_TRIALS = 300;
    private static final int STABILITY_CHECK_INTERVAL = 50;
    private static final double STABILITY_THRESHOLD = 0.02;
    // Boss Checklist draws the drops in a small 5-wide grid; a huge reference loot table (e.g. one that picks
    // from a pool of hundreds of possible items) is capped to the most likely ones instead of listing all of them.
    private static final int MAX_DISPLAYED_DROPS = 24;

    private static final Set<ResourceLocation> warnedMissingTables = ConcurrentHashMap.newKeySet();

    private LootDropsCalculator() {
    }

    /** One loot table and the params to roll it with, restricted to what its own parameter set allows. */
    private record Target(LootTable lootTable, LootParams params) {
    }

    /**
     * One boss being simulated, one trial of its loot table(s) at a time. Also used, with no trials to run at
     * all, to carry a result straight out of {@link DropsSavedData}.
     */
    static final class Session {
        private final String id;
        @Nullable
        private final LivingEntity entity;
        private final List<Target> targets;
        @Nullable
        private final MinecraftServer server;
        @Nullable
        private final String fingerprint;
        @Nullable
        private final BossDrops cachedResult;

        // Its own random source: repeatedly rolling against the world's random sequence would advance and
        // permanently mark that sequence as used, for no benefit (we are not actually dropping anything).
        private final RandomSource random = RandomSource.create();
        private final Map<Item, int[]> tally = new LinkedHashMap<>();
        private final Map<Item, Integer> perTrial = new HashMap<>();
        private int trialsDone;
        private long elapsedNanos;
        private int ticksUsed;

        @Nullable
        private final LevelScaler scaler;
        private final int levelMin;
        private final int levelMax;
        private int lastAppliedLevel = Integer.MIN_VALUE;

        // The chances at the previous stability checkpoint (every STABILITY_CHECK_INTERVAL trials from MIN_TRIALS
        // onward), to see whether the estimate is still moving. Null until the first checkpoint is reached.
        @Nullable
        private Map<Item, Double> lastCheckpoint;
        private boolean stable;

        private Session(String id, @Nullable LivingEntity entity, List<Target> targets, @Nullable MinecraftServer server,
                @Nullable String fingerprint, @Nullable LevelScaler scaler, int levelMin, int levelMax) {
            this.id = id;
            this.entity = entity;
            this.targets = targets;
            this.server = server;
            this.fingerprint = fingerprint;
            this.cachedResult = null;
            this.scaler = scaler;
            this.levelMin = levelMin;
            this.levelMax = levelMax;
        }

        private Session(String id, BossDrops cachedResult) {
            this.id = id;
            this.entity = null;
            this.targets = List.of();
            this.server = null;
            this.fingerprint = null;
            this.cachedResult = cachedResult;
            this.scaler = null;
            this.levelMin = -1;
            this.levelMax = -1;
        }

        boolean isFromCache() {
            return cachedResult != null;
        }

        int trialsDone() {
            return trialsDone;
        }

        long elapsedNanos() {
            return elapsedNanos;
        }

        int ticksUsed() {
            return ticksUsed;
        }

        void markTick() {
            ticksUsed++;
        }

        void runOneTrial() {
            long start = System.nanoTime();
            applyLevelForTrial();
            perTrial.clear();
            for (Target target : targets) {
                for (ItemStack stack : target.lootTable().getRandomItems(target.params(), random)) {
                    if (!stack.isEmpty()) {
                        perTrial.merge(stack.getItem(), stack.getCount(), Integer::sum);
                    }
                }
            }
            for (Map.Entry<Item, Integer> entry : perTrial.entrySet()) {
                int count = entry.getValue();
                int[] data = tally.computeIfAbsent(entry.getKey(), k -> new int[] {0, Integer.MAX_VALUE, 0});
                data[0]++;
                data[1] = Math.min(data[1], count);
                data[2] = Math.max(data[2], count);
            }
            trialsDone++;
            checkStability();
            elapsedNanos += System.nanoTime() - start;
        }

        // From MIN_TRIALS trials onward, every STABILITY_CHECK_INTERVAL trials: if no item newly appeared and no
        // chance moved by more than STABILITY_THRESHOLD since the previous checkpoint, the estimate has settled
        // and the remaining trials (up to MAX_TRIALS) are very unlikely to change the result worth waiting for.
        private void checkStability() {
            if (trialsDone < MIN_TRIALS || (trialsDone - MIN_TRIALS) % STABILITY_CHECK_INTERVAL != 0) {
                return;
            }
            Map<Item, Double> current = new HashMap<>();
            for (Map.Entry<Item, int[]> entry : tally.entrySet()) {
                current.put(entry.getKey(), entry.getValue()[0] / (double) trialsDone);
            }
            if (lastCheckpoint != null) {
                boolean settled = current.keySet().stream().allMatch(lastCheckpoint::containsKey)
                        && current.entrySet().stream().allMatch(e -> Math.abs(e.getValue() - lastCheckpoint.get(e.getKey())) <= STABILITY_THRESHOLD);
                if (settled) {
                    stable = true;
                }
            }
            lastCheckpoint = current;
        }

        // Spreads trials across the boss's level range in a low-discrepancy (van der Corput) order instead of
        // linearly, so the whole range is already fairly well covered after only a few dozen trials - essential
        // for level-gated drops to show up at all when the stability check stops the simulation early.
        private void applyLevelForTrial() {
            if (scaler == null || levelMin < 0) {
                return;
            }
            int level = levelMax == levelMin ? levelMin : levelMin + (int) Math.round(vanDerCorput(trialsDone) * (levelMax - levelMin));
            if (level == lastAppliedLevel) {
                return;
            }
            try {
                scaler.applyLevel(entity, level);
                lastAppliedLevel = level;
            } catch (Throwable t) {
                Constants.debug("Could not apply a level to the drops simulation of '{}'", id, t);
            }
        }

        boolean isDone() {
            return cachedResult != null || stable || trialsDone >= MAX_TRIALS;
        }

        /** Discards the temporary entity (if any) and builds the result. Must only be called once, after isDone(). */
        BossDrops finish() {
            if (entity != null) {
                entity.discard();
            }
            if (cachedResult != null) {
                return cachedResult;
            }

            BossDrops result;
            if (tally.isEmpty()) {
                result = new BossDrops(id, List.of());
            } else {
                List<BossDrop> drops = new ArrayList<>();
                for (Map.Entry<Item, int[]> entry : tally.entrySet()) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
                    int[] data = entry.getValue();
                    drops.add(new BossDrop(itemId.toString(), (double) data[0] / trialsDone, data[1], data[2]));
                }
                drops.sort(Comparator.comparingDouble(BossDrop::chance).reversed());
                if (drops.size() > MAX_DISPLAYED_DROPS) {
                    drops = drops.subList(0, MAX_DISPLAYED_DROPS);
                }
                result = new BossDrops(id, List.copyOf(drops));
            }

            if (fingerprint != null && server != null && Settings.cacheDrops()) {
                try {
                    DropsSavedData.get(server).put(id, fingerprint, result);
                } catch (Throwable t) {
                    Constants.LOGGER.warn("Could not save the simulated drops of '{}'", id, t);
                }
            }
            return result;
        }

        /** Only for abandoning a session early (server stopped/reloaded), does not build or save a result. */
        void discard() {
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static double vanDerCorput(int n) {
        double result = 0.0;
        double f = 0.5;
        n++;
        while (n > 0) {
            result += f * (n & 1);
            n >>>= 1;
            f *= 0.5;
        }
        return result;
    }

    /** Returns null for hidden bosses, unknown entity types and bosses without a resolvable loot table. Never runs a trial. */
    @Nullable
    static Session prepare(String id, MinecraftServer server) {
        try {
            return prepareOrThrow(id, server);
        } catch (Throwable t) {
            Constants.LOGGER.warn("Could not prepare boss drops for '{}'", id, t);
            return null;
        }
    }

    @Nullable
    private static Session prepareOrThrow(String id, MinecraftServer server) {
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

        List<Target> targets = pack != null && !pack.lootTables().isEmpty()
                ? resolveOverrideTargets(id, pack.lootTables(), server, level, entity)
                : resolveDefaultTarget(entity, server, level);
        if (targets == null) {
            entity.discard();
            return null;
        }

        // Only if the entity type has its own leveling settings; a boss whose level "depends on location" is
        // left at no level at all, the same distinction BossStatsCalculator makes for the shown level range.
        LevelScaler scaler = StatsService.createScaler();
        int levelMin = -1;
        int levelMax = -1;
        if (scaler != null) {
            try {
                int[] range = scaler.levelRange(type, level);
                if (range != null) {
                    levelMin = Math.max(0, range[0]);
                    levelMax = Math.max(levelMin, range[1]);
                }
            } catch (Throwable t) {
                Constants.debug("Could not resolve a level range for the drops simulation of '{}'", id, t);
            }
        }

        String fingerprint = null;
        if (Settings.cacheDrops()) {
            List<ResourceLocation> packLootTables = pack != null ? pack.lootTables() : List.of();
            fingerprint = DropsFingerprint.compute(server, targets.stream().map(Target::lootTable).toList(), levelMin, levelMax, packLootTables);
            if (fingerprint != null) {
                BossDrops cached = DropsSavedData.get(server).find(id, fingerprint);
                if (cached != null) {
                    entity.discard();
                    return new Session(id, cached);
                }
            }
        }

        return new Session(id, entity, targets, server, fingerprint, scaler, levelMin, levelMax);
    }

    /** The entity's own loot table. Returns null if it does not have one (e.g. it only hands out loot in code). */
    @Nullable
    private static List<Target> resolveDefaultTarget(LivingEntity entity, MinecraftServer server, ServerLevel level) {
        ResourceKey<LootTable> lootTableKey = entity.getLootTable();
        LootTable lootTable = server.reloadableRegistries().getLootTable(lootTableKey);
        if (lootTable == LootTable.EMPTY) {
            return null;
        }
        return List.of(new Target(lootTable, buildParams(lootTable, level, entity)));
    }

    /**
     * Pack file override: simulate exactly these loot tables instead (e.g. a reward chest a boss fills on death).
     * Returns null, with a one-time warning, if any of them does not exist, so the boss is skipped rather than
     * simulated with only part of its real loot.
     */
    @Nullable
    private static List<Target> resolveOverrideTargets(String id, List<ResourceLocation> lootTableIds,
            MinecraftServer server, ServerLevel level, LivingEntity entity) {
        List<Target> targets = new ArrayList<>(lootTableIds.size());
        for (ResourceLocation lootTableId : lootTableIds) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);
            LootTable lootTable = server.reloadableRegistries().getLootTable(key);
            if (lootTable == LootTable.EMPTY) {
                if (warnedMissingTables.add(lootTableId)) {
                    Constants.LOGGER.warn("Boss pack file for '{}' lists unknown loot table '{}', skipping this boss", id, lootTableId);
                }
                return null;
            }
            targets.add(new Target(lootTable, buildParams(lootTable, level, entity)));
        }
        return targets;
    }

    // A plain player attack without Looting: the same "solo, no bonus" baseline the rest of the addon uses.
    // Only the parameters the table's own parameter set actually allows are set, since LootParams.Builder.create()
    // throws for both a missing required parameter and one that is not allowed at all.
    private static LootParams buildParams(LootTable lootTable, ServerLevel level, LivingEntity entity) {
        LootContextParamSet declared = lootTable.getParamSet();
        // A table without an explicit "type" (some mods' own reward tables, e.g. Bosses of Mass Destruction's
        // chest loot) defaults to ALL_PARAMS, which also requires a block state/tool/explosion radius that make
        // no sense for a boss reward and that we have no sensible value for. Since this is only ever used to
        // simulate boss-reward loot, that default is treated the same as the entity context instead of failing.
        LootContextParamSet paramSet = declared == LootTable.DEFAULT_PARAM_SET ? LootContextParamSets.ENTITY : declared;
        LootParams.Builder builder = new LootParams.Builder(level);
        if (paramSet.isAllowed(LootContextParams.ORIGIN)) {
            builder.withParameter(LootContextParams.ORIGIN, entity.position());
        }
        if (paramSet.isAllowed(LootContextParams.THIS_ENTITY)) {
            builder.withParameter(LootContextParams.THIS_ENTITY, entity);
        }
        if (paramSet.isAllowed(LootContextParams.DAMAGE_SOURCE) || paramSet.isAllowed(LootContextParams.ATTACKING_ENTITY)
                || paramSet.isAllowed(LootContextParams.DIRECT_ATTACKING_ENTITY) || paramSet.isAllowed(LootContextParams.LAST_DAMAGE_PLAYER)) {
            FakePlayer player = FakePlayerFactory.getMinecraft(level);
            if (paramSet.isAllowed(LootContextParams.DAMAGE_SOURCE)) {
                DamageSource damageSource = level.damageSources().playerAttack(player);
                builder.withParameter(LootContextParams.DAMAGE_SOURCE, damageSource);
            }
            if (paramSet.isAllowed(LootContextParams.ATTACKING_ENTITY)) {
                builder.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player);
            }
            if (paramSet.isAllowed(LootContextParams.DIRECT_ATTACKING_ENTITY)) {
                builder.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player);
            }
            if (paramSet.isAllowed(LootContextParams.LAST_DAMAGE_PLAYER)) {
                builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
            }
        }
        return builder.create(paramSet);
    }
}

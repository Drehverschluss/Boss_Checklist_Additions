package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.Settings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Drives {@link LootDropsCalculator} one trial at a time, spread across server ticks within a time budget that
 * adapts to how busy the server currently is. However many bosses there are and however slow a single loot
 * table turns out to be, this never blocks the server for longer than one trial: a running trial is never
 * aborted, the budget only decides whether another one is started this tick.
 */
final class DropsScheduler {
    // budget = clamp(TARGET_MS - average tick time, MIN_MS, MAX_MS), generous since a tick has 50ms in total.
    private static final double TARGET_MS = 40.0;
    private static final double MIN_MS = 2.0;
    private static final double MAX_MS = 15.0;
    private static final double OVERLOADED_MS = 45.0;
    private static final long SLOW_TRIAL_NANOS = 50_000_000L;
    private static final int ORIGIN_SEARCH_RADIUS = 8;
    // No players online (e.g. a dedicated server between restarts): nothing suffers from a slower tick, so the
    // simulation can catch up faster.
    private static final double MAX_MS_IDLE = 30.0;

    private static final Deque<String> pending = new ArrayDeque<>();
    @Nullable
    private static LootDropsCalculator.Session current;
    @Nullable
    private static BlockPos simulationOrigin;
    private static long timeCreditNanos;
    private static boolean warnedSlowTrialThisRun;

    /** A boss whose simulation finished this tick, with enough detail for logging and the cache-hit count. */
    record FinishedBoss(BossDrops drops, int trials, long elapsedNanos, int ticks, boolean fromCache) {
    }

    private DropsScheduler() {
    }

    /** Replaces the queue. Any boss still being simulated is abandoned without a result. */
    static void restart(Collection<String> ids) {
        abandonCurrent();
        pending.clear();
        pending.addAll(ids);
        simulationOrigin = null;
        timeCreditNanos = 0L;
        warnedSlowTrialThisRun = false;
    }

    static void stop() {
        abandonCurrent();
        pending.clear();
        simulationOrigin = null;
        timeCreditNanos = 0L;
    }

    static boolean isIdle() {
        return current == null && pending.isEmpty();
    }

    /** Bosses still waiting (not counting the one currently being simulated). For the debug command. */
    static int queueSize() {
        return pending.size();
    }

    /** Runs trials until the time budget is used up. Returns the bosses that finished during this call, if any. */
    static List<FinishedBoss> tick(MinecraftServer server) {
        if (current == null && pending.isEmpty()) {
            return List.of();
        }

        double averageTickMs = server.getAverageTickTimeNanos() / 1_000_000.0;
        long budgetNanos = computeBudgetNanos(server, averageTickMs);
        timeCreditNanos = Math.min(timeCreditNanos + budgetNanos, 2L * budgetNanos);

        List<FinishedBoss> finished = new ArrayList<>();
        LootDropsCalculator.Session tickedThisCall = current;
        if (current != null) {
            current.markTick();
        }
        if (averageTickMs >= OVERLOADED_MS || timeCreditNanos <= 0L) {
            return finished;
        }

        while (timeCreditNanos > 0L) {
            if (current == null) {
                String id = pending.poll();
                if (id == null) {
                    break;
                }
                current = LootDropsCalculator.prepare(id, server, simulationOrigin(server));
                if (current == null) {
                    // No loot table, hidden, unknown entity, or an invalid pack override: recording an explicit
                    // empty result (instead of just skipping) tells the cache "no drops", so a stale result from
                    // an earlier, still-valid simulation does not linger after a reload. The mixin already treats
                    // an empty result the same as no data at all and keeps Boss Checklist's own list.
                    finished.add(new FinishedBoss(new BossDrops(id, List.of()), 0, 0L, 0, false));
                    continue;
                }
            }

            if (current != tickedThisCall) {
                current.markTick();
                tickedThisCall = current;
            }

            if (!current.isDone()) {
                long trialNanos = current.runOneTrial();
                timeCreditNanos -= trialNanos;
                warnSlowTrial(current, trialNanos);
            }
            if (current.isDone()) {
                try {
                    finished.add(toFinishedBoss(current));
                } catch (Throwable t) {
                    Constants.LOGGER.warn("Could not finish boss drops", t);
                }
                current = null;
            }
        }
        return finished;
    }

    private static FinishedBoss toFinishedBoss(LootDropsCalculator.Session session) {
        boolean fromCache = session.isFromCache();
        BossDrops drops = session.finish();
        return new FinishedBoss(drops, session.trialsDone(), session.elapsedNanos(), session.ticksUsed(), fromCache);
    }

    private static long computeBudgetNanos(MinecraftServer server, double averageTickMs) {
        double max = server.getPlayerList().getPlayerCount() == 0 ? MAX_MS_IDLE : MAX_MS;
        double budgetMs = Math.max(MIN_MS, Math.min(max, TARGET_MS - averageTickMs));
        return (long) (budgetMs * 1_000_000.0);
    }

    private static BlockPos simulationOrigin(MinecraftServer server) {
        if (simulationOrigin != null) {
            return simulationOrigin;
        }

        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        String source = "spawn";
        if (level.hasChunkAt(spawn)) {
            simulationOrigin = spawn.immutable();
        } else {
            ServerPlayer player = level.players().stream().findFirst().orElse(null);
            if (player != null) {
                simulationOrigin = player.blockPosition().immutable();
                source = "player";
            } else {
                simulationOrigin = spawn.immutable();
                source = "spawn, not loaded";
            }
        }

        OriginSearchResult result = findOrigin(level, simulationOrigin);
        simulationOrigin = result.origin();
        Constants.debug("Drops simulation origin: {} {} {} ({}; chunk {} {}, {} structure references, searched {} chunks)",
                simulationOrigin.getX(), simulationOrigin.getY(), simulationOrigin.getZ(), source,
                result.chunkX(), result.chunkZ(), result.structureReferences(), result.searchedChunks());
        return simulationOrigin;
    }

    private record OriginSearchResult(BlockPos origin, int chunkX, int chunkZ, int structureReferences, int searchedChunks) {
    }

    private static OriginSearchResult findOrigin(ServerLevel level, BlockPos start) {
        ChunkPos startChunk = new ChunkPos(start);
        LevelChunk bestChunk = null;
        int fewestReferences = Integer.MAX_VALUE;
        int searchedChunks = 0;

        for (ChunkPos chunkPos : spiralChunks(startChunk)) {
            searchedChunks++;
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null) {
                continue;
            }
            int references = chunk.getAllReferences().values().stream().mapToInt(entries -> entries.size()).sum();
            if (references == 0) {
                return originInChunk(level, start.getY(), chunkPos, 0, searchedChunks);
            }
            if (references < fewestReferences) {
                bestChunk = chunk;
                fewestReferences = references;
            }
        }

        if (bestChunk != null) {
            return originInChunk(level, start.getY(), bestChunk.getPos(), fewestReferences, searchedChunks);
        }
        return new OriginSearchResult(start, startChunk.x, startChunk.z, 0, searchedChunks);
    }

    private static List<ChunkPos> spiralChunks(ChunkPos center) {
        List<ChunkPos> chunks = new ArrayList<>((ORIGIN_SEARCH_RADIUS * 2 + 1) * (ORIGIN_SEARCH_RADIUS * 2 + 1));
        chunks.add(center);
        for (int radius = 1; radius <= ORIGIN_SEARCH_RADIUS; radius++) {
            int minX = center.x - radius;
            int maxX = center.x + radius;
            int minZ = center.z - radius;
            int maxZ = center.z + radius;
            for (int x = minX; x <= maxX; x++) {
                chunks.add(new ChunkPos(x, minZ));
            }
            for (int z = minZ + 1; z <= maxZ; z++) {
                chunks.add(new ChunkPos(maxX, z));
            }
            for (int x = maxX - 1; x >= minX; x--) {
                chunks.add(new ChunkPos(x, maxZ));
            }
            for (int z = maxZ - 1; z > minZ; z--) {
                chunks.add(new ChunkPos(minX, z));
            }
        }
        return chunks;
    }

    private static OriginSearchResult originInChunk(ServerLevel level, int fallbackY, ChunkPos chunkPos, int references, int searchedChunks) {
        int x = chunkPos.x * 16 + 8;
        int z = chunkPos.z * 16 + 8;
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = surfaceY >= level.getMinBuildHeight() && surfaceY < level.getMaxBuildHeight() ? surfaceY : fallbackY;
        return new OriginSearchResult(new BlockPos(x, y, z), chunkPos.x, chunkPos.z, references, searchedChunks);
    }

    private static void warnSlowTrial(LootDropsCalculator.Session session, long trialNanos) {
        if (trialNanos <= SLOW_TRIAL_NANOS) {
            return;
        }
        String millis = String.format(Locale.ROOT, "%.1f", trialNanos / 1_000_000.0);
        String message = "Drops {}: one trial took {} ms - the simulation slows down to protect the server tick";
        if (Settings.debugLogging()) {
            if (session.markSlowTrialWarned()) {
                Constants.debug(message, session.id(), millis);
            }
        } else if (!warnedSlowTrialThisRun) {
            warnedSlowTrialThisRun = true;
            Constants.LOGGER.warn(message, session.id(), millis);
        }
    }

    private static void abandonCurrent() {
        if (current != null) {
            try {
                current.discard();
            } catch (Throwable t) {
                Constants.LOGGER.warn("Could not discard an in-progress boss drops simulation", t);
            }
            current = null;
        }
    }
}

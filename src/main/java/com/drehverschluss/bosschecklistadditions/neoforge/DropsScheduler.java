package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.server.MinecraftServer;

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
    // No players online (e.g. a dedicated server between restarts): nothing suffers from a slower tick, so the
    // simulation can catch up faster.
    private static final double MAX_MS_IDLE = 30.0;

    private static final Deque<String> pending = new ArrayDeque<>();
    @Nullable
    private static LootDropsCalculator.Session current;

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
    }

    static void stop() {
        abandonCurrent();
        pending.clear();
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

        List<FinishedBoss> finished = new ArrayList<>();
        long deadline = System.nanoTime() + computeBudgetNanos(server);
        LootDropsCalculator.Session tickedThisCall = null;
        while (System.nanoTime() < deadline) {
            if (current == null) {
                String id = pending.poll();
                if (id == null) {
                    break;
                }
                current = LootDropsCalculator.prepare(id, server);
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
                current.runOneTrial();
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

    private static long computeBudgetNanos(MinecraftServer server) {
        double averageTickMs = server.getAverageTickTimeNanos() / 1_000_000.0;
        double max = server.getPlayerList().getPlayerCount() == 0 ? MAX_MS_IDLE : MAX_MS;
        double budgetMs = Math.max(MIN_MS, Math.min(max, TARGET_MS - averageTickMs));
        return (long) (budgetMs * 1_000_000.0);
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

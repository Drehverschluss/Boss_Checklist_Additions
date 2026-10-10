package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.ServerBossDropsCache;
import com.drehverschluss.bosschecklistadditions.core.Settings;
import com.drehverschluss.bosschecklistadditions.core.network.BossDropsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossDropsUpdatePayload;
import com.yori3o.boss_checklist.common.server.data.ServerBossIdsLoader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Simulates the boss drops without ever blocking the server: (re)starting puts every known boss into
 * {@link DropsScheduler}'s queue, and {@link #tick} lets it work through a little of that queue each
 * server tick, broadcasting a boss's real drops as soon as it finishes.
 */
public final class DropsService {
    private static volatile long startedAtMillis;
    private static volatile int queuedTotal;
    private static volatile int finishedThisRun;
    private static volatile int fromCacheThisRun;

    private DropsService() {
    }

    /** Queues every boss known to Boss Checklist for a fresh simulation. Previous results stay in use until replaced. */
    public static void restart(MinecraftServer server) {
        try {
            List<String> ids = new ArrayList<>(ServerBossIdsLoader.LOADED_BOSSES);
            ids.sort(String::compareTo);
            DropsFingerprint.refreshGlobalPart(server);
            if (Settings.cacheDrops()) {
                DropsSavedData.get(server).retainOnly(ids);
            }
            DropsScheduler.restart(ids);
            startedAtMillis = System.currentTimeMillis();
            queuedTotal = ids.size();
            finishedThisRun = 0;
            fromCacheThisRun = 0;
            Constants.LOGGER.info("Queued a boss drops simulation for {} boss(es)", queuedTotal);
        } catch (Throwable t) {
            Constants.LOGGER.error("Could not queue boss drops", t);
        }
    }

    /** /bcadditions drops refresh: discards any saved results and simulates every boss again. */
    public static void refresh(MinecraftServer server) {
        try {
            DropsSavedData.get(server).clearAll();
        } catch (Throwable t) {
            Constants.LOGGER.error("Could not clear the saved boss drops", t);
        }
        restart(server);
    }

    /** Call every server tick. Cheap no-op once the queue is empty. */
    public static void tick(MinecraftServer server) {
        if (DropsScheduler.isIdle()) {
            return;
        }
        List<DropsScheduler.FinishedBoss> finished;
        try {
            finished = DropsScheduler.tick(server);
        } catch (Throwable t) {
            Constants.LOGGER.error("Could not simulate boss drops", t);
            return;
        }
        if (!finished.isEmpty()) {
            List<BossDrops> payloadEntries = new ArrayList<>(finished.size());
            for (DropsScheduler.FinishedBoss entry : finished) {
                ServerBossDropsCache.update(entry.drops());
                payloadEntries.add(entry.drops());
                if (!entry.drops().drops().isEmpty()) {
                    finishedThisRun++;
                }
                logFinished(entry);
            }
            Constants.debug("Simulated drops for {} more bosses", finished.size());
            Net.broadcast(server, new BossDropsUpdatePayload(payloadEntries));
        }
        // isIdle() can become true either because the last boss(es) just finished (handled above, which also
        // covers bosses skipped for having no loot table at all) or because the queue was simply empty already.
        if (DropsScheduler.isIdle()) {
            double seconds = (System.currentTimeMillis() - startedAtMillis) / 1000.0;
            Constants.LOGGER.info("Finished simulating boss drops: {} of {} boss(es) have a loot table, {} from the cache (took {})",
                    finishedThisRun, queuedTotal, fromCacheThisRun, String.format(Locale.ROOT, "%.1fs", seconds));
        }
    }

    private static void logFinished(DropsScheduler.FinishedBoss entry) {
        if (entry.fromCache()) {
            fromCacheThisRun++;
            Constants.debug("Drops {}: from the cache", entry.drops().entityId());
            return;
        }
        if (entry.trials() <= 0) {
            return;
        }
        double totalMs = entry.elapsedNanos() / 1_000_000.0;
        double perTrialMs = totalMs / entry.trials();
        Constants.debug("Drops {}: {} trials, {} ms total, {} ms/trial, {} ticks", entry.drops().entityId(), entry.trials(),
                String.format(Locale.ROOT, "%.1f", totalMs), String.format(Locale.ROOT, "%.2f", perTrialMs), entry.ticks());
    }

    public static void stop() {
        DropsScheduler.stop();
    }

    public static void sendAll(ServerPlayer player) {
        Net.send(player, new BossDropsPayload(ServerBossDropsCache.get()));
    }
}

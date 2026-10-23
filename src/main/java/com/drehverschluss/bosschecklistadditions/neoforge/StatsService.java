package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossPackDataManager;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.BossStatsCalculator;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.DungeonScaler;
import com.drehverschluss.bosschecklistadditions.core.LevelScaler;
import com.drehverschluss.bosschecklistadditions.core.ServerBossStatsCache;
import com.drehverschluss.bosschecklistadditions.core.Settings;
import com.drehverschluss.bosschecklistadditions.core.compat.DungeonDifficultyScaler;
import com.drehverschluss.bosschecklistadditions.core.compat.DynamicDifficultyScaler;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsUpdatePayload;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Calculates the boss stats and keeps the server cache and the clients up to date. */
public final class StatsService {
    private StatsService() {
    }

    public static void recalculateAll(MinecraftServer server) {
        try {
            ServerBossStatsCache.set(BossStatsCalculator.calculateAll(server, createScaler(), createDungeonScaler()));
            Constants.debug("Calculated stats for {} bosses", ServerBossStatsCache.get().size());
        } catch (Throwable t) {
            Constants.LOGGER.error("Could not calculate boss stats", t);
        }
    }

    public static void recalculateAndBroadcast(MinecraftServer server) {
        recalculateAll(server);
        Net.broadcast(server, new BossStatsPayload(ServerBossStatsCache.get(), BossPackDataManager.maxTier()));
    }

    /** Recalculates one boss and updates the cache. Returns null if the boss has no stats (hidden, unknown, failed). */
    @Nullable
    public static BossStats recalculateOne(MinecraftServer server, String entityId) {
        BossStats stats = BossStatsCalculator.calculateOne(entityId, server, createScaler(), createDungeonScaler());
        ServerBossStatsCache.update(entityId, stats);
        return stats;
    }

    public static void sendAll(ServerPlayer player) {
        Net.send(player, new BossStatsPayload(ServerBossStatsCache.get(), BossPackDataManager.maxTier()));
    }

    public static void broadcastUpdate(MinecraftServer server, List<BossStats> updated) {
        if (!updated.isEmpty()) {
            Net.broadcast(server, new BossStatsUpdatePayload(updated));
        }
    }

    // The Dynamic Difficulty and Dungeon Difficulty classes are only touched if the mods are loaded.
    // Package-private: LootDropsCalculator reuses this to apply the same level range to its simulated entity.
    @Nullable
    static LevelScaler createScaler() {
        if (ModList.get().isLoaded(Constants.DYNAMIC_DIFFICULTY_ID)) {
            return new DynamicDifficultyScaler();
        }
        return null;
    }

    @Nullable
    private static DungeonScaler createDungeonScaler() {
        if (Settings.enableDungeonDifficulty() && ModList.get().isLoaded(Constants.DUNGEON_DIFFICULTY_ID)) {
            return new DungeonDifficultyScaler();
        }
        return null;
    }
}

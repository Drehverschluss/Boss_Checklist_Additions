package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.List;

import org.jetbrains.annotations.Nullable;

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
import net.neoforged.neoforge.network.PacketDistributor;

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
        PacketDistributor.sendToAllPlayers(new BossStatsPayload(ServerBossStatsCache.get()));
    }

    /** Recalculates one boss and updates the cache. Returns null if the boss has no stats (hidden, unknown, failed). */
    @Nullable
    public static BossStats recalculateOne(MinecraftServer server, String entityId) {
        BossStats stats = BossStatsCalculator.calculateOne(entityId, server, createScaler(), createDungeonScaler());
        ServerBossStatsCache.update(entityId, stats);
        return stats;
    }

    public static void sendAll(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BossStatsPayload(ServerBossStatsCache.get()));
    }

    public static void broadcastUpdate(List<BossStats> updated) {
        if (!updated.isEmpty()) {
            PacketDistributor.sendToAllPlayers(new BossStatsUpdatePayload(updated));
        }
    }

    // The Dynamic Difficulty and Dungeon Difficulty classes are only touched if the mods are loaded.
    @Nullable
    private static LevelScaler createScaler() {
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

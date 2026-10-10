package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.drehverschluss.bosschecklistadditions.core.BaseValueObserver;
import com.drehverschluss.bosschecklistadditions.core.BossPackDataManager;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.ServerBossStatsCache;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerEvents {
    private ServerEvents() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BossPackDataManager());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        StatsService.recalculateAll(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ServerBossStatsCache.clear();
        BossPackDataManager.clear();
        BaseValueObserver.clear();
    }

    // Fires with a null player on /reload, after all data (including the pack files) has been reloaded.
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        StatsService.recalculateAll(event.getPlayerList().getServer());
        BossStatsPayload payload = new BossStatsPayload(ServerBossStatsCache.get());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsService.sendAll(player);
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Entities loaded from a chunk may already be in a later phase and no longer show their real base values.
        if (event.loadedFromDisk()) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            BaseValueObserver.onJoin(event.getEntity(), level.getServer().getTickCount());
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Set<String> changed = BaseValueObserver.process(event.getServer());
        if (changed.isEmpty()) {
            return;
        }
        List<BossStats> updated = new ArrayList<>();
        for (String id : changed) {
            BossStats stats = StatsService.recalculateOne(event.getServer(), id);
            if (stats != null) {
                updated.add(stats);
            }
        }
        StatsService.broadcastUpdate(updated);
    }
}
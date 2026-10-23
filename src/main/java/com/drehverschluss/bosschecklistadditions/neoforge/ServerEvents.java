package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.drehverschluss.bosschecklistadditions.core.BaseValueObserver;
import com.drehverschluss.bosschecklistadditions.core.BossPackDataManager;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.LootResourcesFingerprint;
import com.drehverschluss.bosschecklistadditions.core.ServerBossDropsCache;
import com.drehverschluss.bosschecklistadditions.core.ServerBossStatsCache;
import com.drehverschluss.bosschecklistadditions.core.network.BossDropsPayload;
import com.drehverschluss.bosschecklistadditions.core.network.BossStatsPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ServerEvents {
    private ServerEvents() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BossPackDataManager());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        StatsService.recalculateAll(event.getServer());
        DropsService.restart(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ServerBossStatsCache.clear();
        ServerBossDropsCache.clear();
        DropsService.stop();
        BossPackDataManager.clear();
        LootResourcesFingerprint.clear();
        BaseValueObserver.clear();
        FakePlayerFactory.unloadLevel(event.getServer().overworld());
    }

    // Fires with a null player on /reload, after all data (including the pack files) has been reloaded.
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        StatsService.recalculateAll(event.getPlayerList().getServer());
        BossStatsPayload statsPayload = new BossStatsPayload(ServerBossStatsCache.get(), BossPackDataManager.maxTier());
        // Drops are requeued and trickle in via BossDropsUpdatePayload; players get the (possibly stale) snapshot now.
        BossDropsPayload dropsPayload = new BossDropsPayload(ServerBossDropsCache.get());
        event.getRelevantPlayers().forEach(player -> {
            Net.send(player, statsPayload);
            Net.send(player, dropsPayload);
        });
        DropsService.restart(event.getPlayerList().getServer());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsService.sendAll(player);
            DropsService.sendAll(player);
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
        DropsService.tick(event.getServer());

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
        StatsService.broadcastUpdate(event.getServer(), updated);
    }
}
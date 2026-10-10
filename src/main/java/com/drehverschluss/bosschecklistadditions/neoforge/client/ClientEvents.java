package com.drehverschluss.bosschecklistadditions.neoforge.client;

import com.drehverschluss.bosschecklistadditions.client.ClientBossDropsCache;
import com.drehverschluss.bosschecklistadditions.client.ClientBossStatsCache;
import com.drehverschluss.bosschecklistadditions.client.PackInfoTab;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Only loaded on the physical client. */
public final class ClientEvents {
    private ClientEvents() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouseScrolled);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBossStatsCache.clear();
        ClientBossDropsCache.clear();
    }

    private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (PackInfoTab.onScroll(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }
}

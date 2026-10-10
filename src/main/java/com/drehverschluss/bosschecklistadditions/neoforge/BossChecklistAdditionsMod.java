package com.drehverschluss.bosschecklistadditions.neoforge;

import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.neoforge.client.ClientEvents;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class BossChecklistAdditionsMod {
    public BossChecklistAdditionsMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AdditionsConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AdditionsConfig.SERVER_SPEC);
        modEventBus.addListener(AdditionsConfig::onLoading);
        modEventBus.addListener(AdditionsConfig::onReloading);

        modEventBus.addListener(NetworkEvents::onRegisterPayloads);

        NeoForge.EVENT_BUS.addListener(ServerEvents::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(DebugCommand::onRegisterCommands);

        if (FMLEnvironment.dist.isClient()) {
            ClientEvents.init();
        }
    }
}

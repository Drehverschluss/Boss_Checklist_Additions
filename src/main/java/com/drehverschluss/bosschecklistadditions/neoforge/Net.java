package com.drehverschluss.bosschecklistadditions.neoforge;

import com.drehverschluss.bosschecklistadditions.core.Constants;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends our optional payloads only to players whose client actually negotiated the channel. NeoForge's own
 * PacketDistributor does not check this: sending a payload on a channel the client does not have throws
 * (ServerCommonPacketListenerImpl.send() -&gt; NetworkRegistry.checkPacket()), which would otherwise crash the
 * server for every player without the addon (or with an incompatible version) as soon as anything is broadcast.
 */
final class Net {
    private Net() {
    }

    static void send(ServerPlayer player, CustomPacketPayload payload) {
        try {
            if (player.connection.hasChannel(payload)) {
                player.connection.send(payload);
            }
        } catch (Throwable t) {
            Constants.LOGGER.warn("Could not send {} to {}", payload.type().id(), player.getGameProfile().getName(), t);
        }
    }

    static void broadcast(MinecraftServer server, CustomPacketPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }
}

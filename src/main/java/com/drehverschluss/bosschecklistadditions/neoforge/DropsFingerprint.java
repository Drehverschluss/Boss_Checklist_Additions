package com.drehverschluss.bosschecklistadditions.neoforge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.LootResourcesFingerprint;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;

/**
 * Builds the fingerprint a simulated boss's drops are cached under: anything that could plausibly change what a
 * boss's loot table actually contains. A config file of some other mod (e.g. an RPG class mod's own loot pools)
 * cannot be read reliably, so it is not part of this - {@code /bcadditions drops refresh} is the escape hatch for
 * changes that only show up there.
 */
final class DropsFingerprint {
    // Bump this when the simulation itself changes in a way that could change its results (e.g. a different
    // random distribution, a different trial count), so old cached results are not trusted anymore.
    private static final int SIMULATION_VERSION = 1;

    private static volatile String globalPart = "";

    private DropsFingerprint() {
    }

    /** Mods and datapacks rarely change between bosses of the same run, so this is computed once per restart. */
    static void refreshGlobalPart(MinecraftServer server) {
        String mods = ModList.get().getMods().stream()
                .map(mod -> mod.getModId() + "@" + mod.getVersion())
                .sorted()
                .collect(Collectors.joining(","));
        String datapacks = server.getPackRepository().getSelectedIds().stream().sorted().collect(Collectors.joining(","));
        globalPart = "sim" + SIMULATION_VERSION + ";mods=" + mods + ";packs=" + datapacks;
    }

    /**
     * Null if any of the loot tables could not be serialized (e.g. an unusual custom entry type), so this boss
     * is never read from or written to the cache instead of risking a fingerprint that looks stable but is not.
     */
    @Nullable
    static String compute(MinecraftServer server, List<LootTable> lootTables, int levelMin, int levelMax, List<ResourceLocation> packLootTables) {
        StringBuilder text = new StringBuilder(globalPart);
        text.append(";loot=").append(LootResourcesFingerprint.get());
        text.append(";level=").append(levelMin).append(':').append(levelMax);
        text.append(";pack=").append(packLootTables.stream().map(Object::toString).sorted().collect(Collectors.joining(",")));

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        for (LootTable lootTable : lootTables) {
            DataResult<JsonElement> encoded = LootTable.DIRECT_CODEC.encodeStart(ops, lootTable);
            Optional<JsonElement> json = encoded.result();
            if (json.isEmpty()) {
                return null;
            }
            text.append(";table=").append(json.get());
        }
        return sha1(text.toString());
    }

    private static String sha1(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Constants.LOGGER.warn("Could not hash a boss drops fingerprint", e);
            return null;
        }
    }
}

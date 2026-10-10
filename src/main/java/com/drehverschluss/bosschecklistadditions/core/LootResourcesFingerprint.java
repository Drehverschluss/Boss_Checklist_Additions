package com.drehverschluss.bosschecklistadditions.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A hash over the content of every resource under {@code data/<any namespace>/loot_modifiers/} and
 * {@code data/lootintegrations/} (any path). Global Loot Modifiers are not part of a loot table itself, and a
 * KubeJS pack's own id does not change when a file inside it changes, so neither is otherwise visible to the
 * per-boss drops cache fingerprint. Recomputed once per datapack reload, not per boss.
 */
public final class LootResourcesFingerprint {
    private static volatile String value = "";

    private LootResourcesFingerprint() {
    }

    public static String get() {
        return value;
    }

    public static void clear() {
        value = "";
    }

    public static void recompute(ResourceManager resourceManager) {
        try {
            Map<ResourceLocation, Resource> resources = new TreeMap<>(Comparator.comparing(ResourceLocation::toString));
            resources.putAll(resourceManager.listResources("loot_modifiers", location -> true));
            resources.putAll(resourceManager.listResources("", location -> location.getNamespace().equals("lootintegrations")));

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                digest.update(entry.getKey().toString().getBytes(StandardCharsets.UTF_8));
                try (InputStream in = entry.getValue().open()) {
                    digest.update(in.readAllBytes());
                }
            }
            value = HexFormat.of().formatHex(digest.digest());
            Constants.debug("Loot resources fingerprint: {} file(s), {}", resources.size(), value);
        } catch (Exception e) {
            Constants.LOGGER.warn("Could not compute the loot resources fingerprint, boss drops will not be cached", e);
            // Never matches a stored value, so every boss falls back to a fresh simulation instead of a wrong cache hit.
            value = "error-" + System.nanoTime();
        }
    }
}

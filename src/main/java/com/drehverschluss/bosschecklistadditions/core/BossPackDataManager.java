package com.drehverschluss.bosschecklistadditions.core;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the pack files of all datapacks. If several files describe the same entity, the last one (sorted by file id) wins. */
public final class BossPackDataManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "boss_checklist_additions/bosses";

    private static volatile Map<String, BossPackData> data = Map.of();
    // The highest "tier" seen across all loaded pack files, 0 if none of them set one. Tiers are entirely
    // defined by the pack (the addon has no idea what they mean), so this is just the range a pack is using.
    private static volatile int maxTier = 0;

    public BossPackDataManager() {
        super(new Gson(), DIRECTORY);
    }

    @Nullable
    public static BossPackData get(String entityId) {
        return data.get(entityId);
    }

    public static int maxTier() {
        return maxTier;
    }

    public static void clear() {
        data = Map.of();
        maxTier = 0;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, BossPackData> loaded = new HashMap<>();
        // Sorted so that the "last file wins" rule is deterministic.
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            ResourceLocation id = file.getKey();
            String fileName = "data/" + id.getNamespace() + "/" + DIRECTORY + "/" + id.getPath() + ".json";
            try {
                DataResult<BossPackData> result = BossPackData.CODEC.parse(JsonOps.INSTANCE, sanitizeTier(file.getValue(), fileName));
                if (result.error().isPresent()) {
                    Constants.LOGGER.warn("Skipping invalid boss pack file {}: {}", fileName, result.error().get().message());
                    continue;
                }
                BossPackData parsed = result.result().orElseThrow();
                BossPackData previous = loaded.put(parsed.entity().toString(), parsed);
                if (previous != null) {
                    Constants.debug("{} replaces an earlier pack file for {}", fileName, parsed.entity());
                }
            } catch (Exception e) {
                Constants.LOGGER.warn("Skipping unreadable boss pack file {}", fileName, e);
            }
        }
        data = Map.copyOf(loaded);
        maxTier = loaded.values().stream().mapToInt(pack -> pack.tier().orElse(0)).max().orElse(0);
        Constants.debug("Loaded {} boss pack files", data.size());
        // Reuses this reload's ResourceManager, since Global Loot Modifiers and LootIntegrations files are
        // not covered by anything else that is recomputed on reload.
        LootResourcesFingerprint.recompute(resourceManager);
    }

    /**
     * "tier" is a lot more likely to have a typo than the other fields (packs copy it between many boss files
     * with different numbers), so an invalid value does not fail the whole file like other invalid fields would:
     * it is just removed here (with one warning), before the regular codec ever sees it.
     */
    private static JsonElement sanitizeTier(JsonElement element, String fileName) {
        if (!(element instanceof JsonObject object) || !object.has("tier")) {
            return element;
        }
        JsonElement tierElement = object.get("tier");
        Integer tier = tierElement.isJsonPrimitive() && tierElement.getAsJsonPrimitive().isNumber() ? asWholeNumber(tierElement) : null;
        if (tier != null && tier >= 1) {
            return element;
        }
        Constants.LOGGER.warn("Ignoring invalid 'tier' in boss pack file {}: must be a whole number >= 1", fileName);
        JsonObject copy = object.deepCopy();
        copy.remove("tier");
        return copy;
    }

    @Nullable
    private static Integer asWholeNumber(JsonElement element) {
        try {
            double value = element.getAsDouble();
            int rounded = element.getAsInt();
            return value == rounded ? rounded : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

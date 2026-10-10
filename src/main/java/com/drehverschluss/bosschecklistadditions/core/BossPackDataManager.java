package com.drehverschluss.bosschecklistadditions.core;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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

    public BossPackDataManager() {
        super(new Gson(), DIRECTORY);
    }

    @Nullable
    public static BossPackData get(String entityId) {
        return data.get(entityId);
    }

    public static void clear() {
        data = Map.of();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, BossPackData> loaded = new HashMap<>();
        // Sorted so that the "last file wins" rule is deterministic.
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            ResourceLocation id = file.getKey();
            String fileName = "data/" + id.getNamespace() + "/" + DIRECTORY + "/" + id.getPath() + ".json";
            try {
                DataResult<BossPackData> result = BossPackData.CODEC.parse(JsonOps.INSTANCE, file.getValue());
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
        Constants.debug("Loaded {} boss pack files", data.size());
    }
}

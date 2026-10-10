package com.drehverschluss.bosschecklistadditions.core;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Base attributes seen on real boss entities, per entity type. Stored in the overworld data storage. */
public class ObservedBaseValues extends SavedData {
    public static final String NAME = "boss_checklist_additions_observed";

    private static final String KEY = "entries";
    private static final Codec<Map<String, BaseAttributes>> CODEC = Codec.unboundedMap(Codec.STRING, BaseAttributes.CODEC);

    private final Map<String, BaseAttributes> values = new TreeMap<>();

    public static ObservedBaseValues get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static Factory<ObservedBaseValues> factory() {
        return new Factory<>(ObservedBaseValues::new, ObservedBaseValues::load, null);
    }

    private static ObservedBaseValues load(CompoundTag tag, HolderLookup.Provider registries) {
        ObservedBaseValues result = new ObservedBaseValues();
        Tag entries = tag.get(KEY);
        if (entries != null) {
            CODEC.parse(NbtOps.INSTANCE, entries)
                    .resultOrPartial(error -> Constants.LOGGER.warn("Could not read observed boss values: {}", error))
                    .ifPresent(result.values::putAll);
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, values)
                .resultOrPartial(error -> Constants.LOGGER.warn("Could not save observed boss values: {}", error))
                .ifPresent(encoded -> tag.put(KEY, encoded));
        return tag;
    }

    @Nullable
    public BaseAttributes get(String entityId) {
        return values.get(entityId);
    }

    public Map<String, BaseAttributes> all() {
        return Collections.unmodifiableMap(values);
    }

    /** Stores the newest values. Returns true if they differ from what was stored before. */
    public boolean observe(String entityId, BaseAttributes attributes) {
        BaseAttributes previous = values.put(entityId, attributes);
        if (attributes.equals(previous)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean forget(String entityId) {
        if (values.remove(entityId) == null) {
            return false;
        }
        setDirty();
        return true;
    }
}

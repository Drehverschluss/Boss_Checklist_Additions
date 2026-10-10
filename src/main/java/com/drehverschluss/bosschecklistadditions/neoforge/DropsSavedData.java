package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.drehverschluss.bosschecklistadditions.core.BossDrops;
import com.drehverschluss.bosschecklistadditions.core.Constants;

import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Finished boss drops, saved in the overworld so a server does not have to simulate everything again on every
 * start or reload. Each entry is only used while its fingerprint still matches (see {@link DropsFingerprint}).
 */
public final class DropsSavedData extends SavedData {
    private static final String ID = "boss_checklist_additions_drops";
    static final SavedData.Factory<DropsSavedData> FACTORY =
            new SavedData.Factory<>(DropsSavedData::new, DropsSavedData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Entry> entries = new HashMap<>();

    private record Entry(String fingerprint, BossDrops drops) {
    }

    static DropsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    /** The cached drops of this boss, only if its fingerprint still matches what it was simulated with. */
    @Nullable
    BossDrops find(String bossId, String fingerprint) {
        Entry entry = entries.get(bossId);
        return entry != null && entry.fingerprint().equals(fingerprint) ? entry.drops() : null;
    }

    void put(String bossId, String fingerprint, BossDrops drops) {
        entries.put(bossId, new Entry(fingerprint, drops));
        setDirty();
    }

    /** Drops entries of bosses Boss Checklist no longer knows about, so the file does not grow forever. */
    void retainOnly(Collection<String> knownIds) {
        if (entries.keySet().retainAll(new HashSet<>(knownIds))) {
            setDirty();
        }
    }

    /** For /bcadditions drops refresh and cacheDrops = false. */
    void clearAll() {
        if (!entries.isEmpty()) {
            entries.clear();
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.getKey());
            entryTag.putString("fingerprint", entry.getValue().fingerprint());
            entryTag.putByteArray("drops", writeDrops(entry.getValue().drops()));
            list.add(entryTag);
        }
        tag.put("bosses", list);
        return tag;
    }

    private static DropsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DropsSavedData data = new DropsSavedData();
        for (Tag rawEntry : tag.getList("bosses", Tag.TAG_COMPOUND)) {
            CompoundTag entryTag = (CompoundTag) rawEntry;
            try {
                String id = entryTag.getString("id");
                String fingerprint = entryTag.getString("fingerprint");
                BossDrops drops = readDrops(id, entryTag.getByteArray("drops"));
                data.entries.put(id, new Entry(fingerprint, drops));
            } catch (Exception e) {
                Constants.LOGGER.warn("Skipping an unreadable saved boss drops entry", e);
            }
        }
        return data;
    }

    // Reuses BossDrops' own network (de)serialization instead of a second, parallel NBT format.
    private static byte[] writeDrops(BossDrops drops) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            drops.write(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    private static BossDrops readDrops(String id, byte[] bytes) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            BossDrops drops = BossDrops.read(buf);
            if (!drops.entityId().equals(id)) {
                throw new IllegalStateException("Entity id mismatch: " + id + " != " + drops.entityId());
            }
            return drops;
        } finally {
            buf.release();
        }
    }
}

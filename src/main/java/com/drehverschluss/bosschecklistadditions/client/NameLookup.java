package com.drehverschluss.bosschecklistadditions.client;

import java.util.Locale;

import com.drehverschluss.bosschecklistadditions.core.BossZone;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Display names of dimensions, structures, biomes and zones. A translation is used if there is one,
 * otherwise the id is made readable ("the_nether" becomes "The Nether").
 */
public final class NameLookup {
    private NameLookup() {
    }

    public static Component dimension(ResourceLocation id) {
        return named("boss_checklist_additions.dimension." + key(id), id);
    }

    // Same keys as Explorer's Compass uses, so a pack only has to translate its structures once.
    public static Component structure(ResourceLocation id) {
        return named("structure." + key(id), id);
    }

    public static Component biome(ResourceLocation id) {
        return named("biome." + key(id), id);
    }

    /**
     * "Boss 5": the difficulty type of Dungeon Difficulty with its level. Dungeon Difficulty's own translation takes the level
     * as its argument (and may have none, like "Settlement"), so the level is only appended for the fallback name.
     */
    public static Component zone(BossZone zone) {
        String key = zone.typeKey();
        if (Language.getInstance().has(key)) {
            return Component.translatable(key, String.valueOf(zone.level()));
        }
        return Component.literal(readable(key.substring(key.lastIndexOf('.') + 1)) + " " + zone.level());
    }
    public static String readable(String path) {
        StringBuilder builder = new StringBuilder();
        for (String word : path.split("[_/\\s]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String key(ResourceLocation id) {
        return id.getNamespace() + "." + id.getPath();
    }

    private static Component named(String key, ResourceLocation id) {
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(readable(id.getPath()));
    }
}
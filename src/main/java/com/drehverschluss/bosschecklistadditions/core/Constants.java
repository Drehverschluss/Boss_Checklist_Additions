package com.drehverschluss.bosschecklistadditions.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

public final class Constants {
    public static final String MOD_ID = "boss_checklist_additions";
    public static final String DYNAMIC_DIFFICULTY_ID = "dynamic_difficulty";
    public static final String DUNGEON_DIFFICULTY_ID = "dungeon_difficulty";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Constants() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Only logged if the debugLogging option of the server config is on. */
    public static void debug(String message, Object... arguments) {
        if (Settings.debugLogging()) {
            LOGGER.info("[debug] " + message, arguments);
        }
    }
}
package com.drehverschluss.bosschecklistadditions.neoforge;

import com.drehverschluss.bosschecklistadditions.core.Settings;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class AdditionsConfig {
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue HIDE_MISSING_ENTITIES = CLIENT_BUILDER
            .comment("Remove Boss Checklist entries whose entity does not exist in the registry (e.g. outdated default entries).")
            .translation("boss_checklist_additions.configuration.hideMissingEntities")
            .define("hideMissingEntities", true);

    private static final ModConfigSpec.BooleanValue REPLACE_STATS = CLIENT_BUILDER
            .comment("Replace the health and armor shown in the boss info screen with the real values of the modpack (solo).")
            .translation("boss_checklist_additions.configuration.replaceStats")
            .define("replaceStats", true);

    private static final ModConfigSpec.BooleanValue SHOW_PACK_INFO_TAB = CLIENT_BUILDER
            .comment("Show the Pack Info bookmark in the boss info screen. The replaced values in the tooltip stay.")
            .translation("boss_checklist_additions.configuration.showPackInfoTab")
            .define("showPackInfoTab", true);

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue OBSERVE_BASE_VALUES = SERVER_BUILDER
            .comment("Remember the base attributes of bosses that are spawned in the world, for bosses that set their real values only on spawn.",
                    "Values that were already observed stay in use, /bcadditions forget removes them.")
            .translation("boss_checklist_additions.configuration.observeBaseValues")
            .define("observeBaseValues", true);

    private static final ModConfigSpec.BooleanValue ENABLE_DUNGEON_DIFFICULTY = SERVER_BUILDER
            .comment("Include the zone and the attribute modifiers of Dungeon Difficulty in the boss stats (needs Dungeon Difficulty).")
            .translation("boss_checklist_additions.configuration.enableDungeonDifficulty")
            .define("enableDungeonDifficulty", true);

    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING = SERVER_BUILDER
            .comment("Write additional information about the stat calculation to the log.")
            .translation("boss_checklist_additions.configuration.debugLogging")
            .define("debugLogging", false);

    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    private AdditionsConfig() {
    }

    public static void onLoading(ModConfigEvent.Loading event) {
        apply(event.getConfig().getSpec());
    }

    public static void onReloading(ModConfigEvent.Reloading event) {
        apply(event.getConfig().getSpec());
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            recalculateLater();
        }
    }

    private static void apply(Object spec) {
        if (spec == CLIENT_SPEC) {
            Settings.setClient(HIDE_MISSING_ENTITIES.get(), REPLACE_STATS.get(), SHOW_PACK_INFO_TAB.get());
        } else if (spec == SERVER_SPEC) {
            Settings.setServer(OBSERVE_BASE_VALUES.get(), ENABLE_DUNGEON_DIFFICULTY.get(), DEBUG_LOGGING.get());
        }
    }

    // The config file is reloaded on another thread, the stats have to be recalculated on the server thread.
    private static void recalculateLater() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> StatsService.recalculateAndBroadcast(server));
        }
    }
}
package com.drehverschluss.bosschecklistadditions.core;

/**
 * Current values of the config options, copied here by the NeoForge config code so that the rest of the mod does not
 * depend on NeoForge. The defaults are used until a config file has been loaded.
 */
public final class Settings {
    public static final int DEFAULT_MINIMUM_DROP_TRIALS = 100;
    public static final int DEFAULT_MAXIMUM_DROP_TRIALS = 300;

    private static volatile boolean hideMissingEntities = true;
    private static volatile boolean replaceStats = true;
    private static volatile boolean showPackInfoTab = true;
    private static volatile boolean replaceDrops = true;

    private static volatile boolean observeBaseValues = true;
    private static volatile boolean enableDungeonDifficulty = true;
    private static volatile boolean debugLogging = false;
    private static volatile boolean cacheDrops = true;
    private static volatile int minimumDropTrials = DEFAULT_MINIMUM_DROP_TRIALS;
    private static volatile int maximumDropTrials = DEFAULT_MAXIMUM_DROP_TRIALS;

    private Settings() {
    }

    public static boolean hideMissingEntities() {
        return hideMissingEntities;
    }

    public static boolean replaceStats() {
        return replaceStats;
    }

    public static boolean showPackInfoTab() {
        return showPackInfoTab;
    }

    public static boolean replaceDrops() {
        return replaceDrops;
    }

    public static boolean observeBaseValues() {
        return observeBaseValues;
    }

    public static boolean enableDungeonDifficulty() {
        return enableDungeonDifficulty;
    }

    public static boolean debugLogging() {
        return debugLogging;
    }

    public static boolean cacheDrops() {
        return cacheDrops;
    }

    public static int minimumDropTrials() {
        return minimumDropTrials;
    }

    public static int maximumDropTrials() {
        return maximumDropTrials;
    }

    public static void setClient(boolean hideMissingEntities, boolean replaceStats, boolean showPackInfoTab, boolean replaceDrops) {
        Settings.hideMissingEntities = hideMissingEntities;
        Settings.replaceStats = replaceStats;
        Settings.showPackInfoTab = showPackInfoTab;
        Settings.replaceDrops = replaceDrops;
    }

    public static void setServer(boolean observeBaseValues, boolean enableDungeonDifficulty, boolean debugLogging, boolean cacheDrops,
            int minimumDropTrials, int maximumDropTrials) {
        Settings.observeBaseValues = observeBaseValues;
        Settings.enableDungeonDifficulty = enableDungeonDifficulty;
        Settings.debugLogging = debugLogging;
        Settings.cacheDrops = cacheDrops;
        Settings.minimumDropTrials = minimumDropTrials;
        Settings.maximumDropTrials = maximumDropTrials;
    }
}
package com.drehverschluss.bosschecklistadditions.client;

import java.util.Locale;

import com.drehverschluss.bosschecklistadditions.core.BossStats;

/** Number formatting shared by the health tooltip and the Pack Info tab. */
public final class StatFormat {
    public static final String RANGE_SEPARATOR = "\u2013";
    public static final String APPROXIMATE = "\u2248";

    private StatFormat() {
    }

    /** Whole numbers, used for health. */
    public static String whole(double min, double max) {
        return range(String.valueOf(Math.round(Math.min(min, max))), String.valueOf(Math.round(Math.max(min, max))));
    }

    /** One decimal place for values that are not whole numbers (1.5), used for armor and attack. */
    public static String decimal(double min, double max) {
        return range(decimal(Math.min(min, max)), decimal(Math.max(min, max)));
    }

    /** Prefix for values that contain a random modifier. */
    public static String approximate(boolean approximate) {
        return approximate ? APPROXIMATE : "";
    }

    public static String level(BossStats stats) {
        return range(String.valueOf(stats.levelMin()), String.valueOf(stats.levelMax()));
    }

    public static String decimal(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.format(Locale.ROOT, "%.1f", rounded);
    }

    private static String range(String low, String high) {
        return low.equals(high) ? high : low + RANGE_SEPARATOR + high;
    }
}
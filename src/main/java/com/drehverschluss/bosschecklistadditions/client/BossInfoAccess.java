package com.drehverschluss.bosschecklistadditions.client;

import java.lang.reflect.Field;

import com.yori3o.boss_checklist.common.client.gui.BossInfoScreen;

/**
 * Boss Checklist's InfoTab enum is package-private, so the current tab can only be reached by reflection.
 * Any failure is reported to the caller, which then falls back to not touching the tab.
 */
public final class BossInfoAccess {
    private static Field tabField;
    private static Object noneConstant;

    private BossInfoAccess() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized void init() throws ReflectiveOperationException {
        if (tabField != null) {
            return;
        }
        Field field = BossInfoScreen.class.getDeclaredField("currentTab");
        field.setAccessible(true);
        noneConstant = Enum.valueOf((Class) field.getType(), "NONE");
        tabField = field;
    }

    public static boolean isTabNone(BossInfoScreen screen) throws ReflectiveOperationException {
        init();
        return tabField.get(screen) == noneConstant;
    }

    public static void resetTab(BossInfoScreen screen) throws ReflectiveOperationException {
        init();
        tabField.set(screen, noneConstant);
    }
}
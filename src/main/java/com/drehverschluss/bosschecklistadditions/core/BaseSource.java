package com.drehverschluss.bosschecklistadditions.core;

/** Where the base attributes of a boss came from. Ordered by priority, the highest ordinal wins. */
public enum BaseSource {
    ENTITY,
    OBSERVED,
    PACK;

    private static final BaseSource[] VALUES = values();

    public static BaseSource byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : ENTITY;
    }

    public BaseSource max(BaseSource other) {
        return other.ordinal() > ordinal() ? other : this;
    }
}

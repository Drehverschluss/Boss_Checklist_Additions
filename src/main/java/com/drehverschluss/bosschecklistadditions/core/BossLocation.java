package com.drehverschluss.bosschecklistadditions.core;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Where a boss is found, as described by its pack file. */
public record BossLocation(Optional<ResourceLocation> dimension, Optional<BossPackData.LocationType> type, Optional<ResourceLocation> id) {

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(dimension, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(type, FriendlyByteBuf::writeEnum);
        buf.writeOptional(id, FriendlyByteBuf::writeResourceLocation);
    }

    public static BossLocation read(FriendlyByteBuf buf) {
        return new BossLocation(
                buf.readOptional(FriendlyByteBuf::readResourceLocation),
                buf.readOptional(reader -> reader.readEnum(BossPackData.LocationType.class)),
                buf.readOptional(FriendlyByteBuf::readResourceLocation));
    }
}
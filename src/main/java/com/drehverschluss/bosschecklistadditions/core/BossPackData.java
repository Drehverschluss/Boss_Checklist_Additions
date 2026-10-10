package com.drehverschluss.bosschecklistadditions.core;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * One pack file: data/&lt;namespace&gt;/boss_checklist_additions/bosses/&lt;name&gt;.json.
 * The field "quests" is reserved: it is accepted in a file, but ignored.
 */
public record BossPackData(
        ResourceLocation entity,
        Optional<ResourceLocation> dimension,
        Optional<Location> location,
        Optional<BaseAttributes> baseAttributes,
        boolean hidden) {

    public static final Codec<BossPackData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entity").forGetter(BossPackData::entity),
            ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(BossPackData::dimension),
            Location.CODEC.optionalFieldOf("location").forGetter(BossPackData::location),
            BaseAttributes.CODEC.optionalFieldOf("base_attributes").forGetter(BossPackData::baseAttributes),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(BossPackData::hidden))
            .apply(instance, BossPackData::new));

    public record Location(LocationType type, Optional<ResourceLocation> id) {
        public static final Codec<Location> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LocationType.CODEC.fieldOf("type").forGetter(Location::type),
                ResourceLocation.CODEC.optionalFieldOf("id").forGetter(Location::id))
                .apply(instance, Location::new));
    }

    public enum LocationType implements StringRepresentable {
        STRUCTURE("structure"),
        BIOME("biome"),
        SUMMONED("summoned"),
        NATURAL("natural");

        public static final Codec<LocationType> CODEC = StringRepresentable.fromEnum(LocationType::values);

        private final String name;

        LocationType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}

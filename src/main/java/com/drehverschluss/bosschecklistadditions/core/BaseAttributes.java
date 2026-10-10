package com.drehverschluss.bosschecklistadditions.core;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Base values of the attributes relevant for the boss stats. Every value is optional. */
public record BaseAttributes(Optional<Double> maxHealth, Optional<Double> armor, Optional<Double> attackDamage) {
    public static final BaseAttributes EMPTY = new BaseAttributes(Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<BaseAttributes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("max_health").forGetter(BaseAttributes::maxHealth),
            Codec.DOUBLE.optionalFieldOf("armor").forGetter(BaseAttributes::armor),
            Codec.DOUBLE.optionalFieldOf("attack_damage").forGetter(BaseAttributes::attackDamage))
            .apply(instance, BaseAttributes::new));
}

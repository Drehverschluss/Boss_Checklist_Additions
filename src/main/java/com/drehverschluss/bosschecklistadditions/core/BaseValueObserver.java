package com.drehverschluss.bosschecklistadditions.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.yori3o.boss_checklist.common.server.data.ServerBossIdsLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Some bosses only set their real base attributes when they are spawned (e.g. from their own config).
 * Bosses that join a level are read one tick later and their base values (without modifiers) are remembered.
 * Only used on the server thread.
 */
public final class BaseValueObserver {
    private record Pending(LivingEntity entity, int joinTick) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private BaseValueObserver() {
    }

    public static void onJoin(Entity entity, int currentTick) {
        if (!Settings.observeBaseValues() || !(entity instanceof LivingEntity living)) {
            return;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (ServerBossIdsLoader.LOADED_BOSSES.contains(id)) {
            PENDING.add(new Pending(living, currentTick));
        }
    }

    /** Reads all entities that joined in an earlier tick. Returns the ids whose stored values changed. */
    public static Set<String> process(MinecraftServer server) {
        Set<String> changed = new LinkedHashSet<>();
        if (PENDING.isEmpty()) {
            return changed;
        }
        int tick = server.getTickCount();
        ObservedBaseValues store = null;

        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (pending.joinTick() >= tick) {
                continue;
            }
            iterator.remove();
            LivingEntity entity = pending.entity();
            if (entity.isRemoved()) {
                continue;
            }
            try {
                if (store == null) {
                    store = ObservedBaseValues.get(server);
                }
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                BaseAttributes values = new BaseAttributes(
                        baseValue(entity, Attributes.MAX_HEALTH),
                        baseValue(entity, Attributes.ARMOR),
                        baseValue(entity, Attributes.ATTACK_DAMAGE));
                if (store.observe(id, values)) {
                    changed.add(id);
                }
            } catch (Throwable t) {
                Constants.LOGGER.warn("Could not observe base values of {}", entity.getType(), t);
            }
        }
        return changed;
    }

    public static void clear() {
        PENDING.clear();
    }

    // getBaseValue, not getValue: modifiers of the leveling mods must not be included.
    private static Optional<Double> baseValue(LivingEntity entity, Holder<Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? Optional.empty() : Optional.of(instance.getBaseValue());
    }
}
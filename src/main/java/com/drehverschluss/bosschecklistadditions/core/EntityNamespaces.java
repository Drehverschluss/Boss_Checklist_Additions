package com.drehverschluss.bosschecklistadditions.core;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Namespace lookups used to keep boss entries whose entity namespace differs from the mod id of the providing mod. */
public final class EntityNamespaces {
    private static volatile Set<String> namespaces;

    private EntityNamespaces() {
    }

    public static boolean hasEntityType(String namespace) {
        Set<String> cached = namespaces;
        if (cached == null) {
            cached = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .map(ResourceLocation::getNamespace)
                    .collect(Collectors.toUnmodifiableSet());
            namespaces = cached;
        }
        return cached.contains(namespace);
    }

    /** Returns false only if the id is a valid resource location that has no entity type. */
    public static boolean entityExists(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null || BuiltInRegistries.ENTITY_TYPE.containsKey(location);
    }
}

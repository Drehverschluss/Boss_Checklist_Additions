package com.drehverschluss.bosschecklistadditions.neoforge;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.drehverschluss.bosschecklistadditions.core.BaseAttributes;
import com.drehverschluss.bosschecklistadditions.core.BossLocation;
import com.drehverschluss.bosschecklistadditions.core.BossStats;
import com.drehverschluss.bosschecklistadditions.core.Constants;
import com.drehverschluss.bosschecklistadditions.core.ObservedBaseValues;
import com.drehverschluss.bosschecklistadditions.core.ServerBossStatsCache;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DebugCommand {
    private static final double MAX_DISTANCE = 32.0;

    private DebugCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bcadditions")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dump").executes(DebugCommand::dump))
                .then(Commands.literal("observed").executes(DebugCommand::observed))
                .then(Commands.literal("compare").executes(DebugCommand::compare))
                .then(Commands.literal("forget")
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        ObservedBaseValues.get(context.getSource().getServer()).all().keySet(), builder))
                                .executes(DebugCommand::forget))));
    }

    private static int dump(CommandContext<CommandSourceStack> context) {
        List<BossStats> stats = ServerBossStatsCache.get();
        CommandSourceStack source = context.getSource();
        if (stats.isEmpty()) {
            send(source, "No boss stats calculated yet.");
            return 0;
        }

        send(source, "Boss stats (" + stats.size() + " bosses, solo):");
        for (BossStats entry : stats) {
            String line = format(entry);
            Constants.LOGGER.info(line);
            send(source, line);
        }
        return stats.size();
    }

    private static int observed(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<String, BaseAttributes> observed = ObservedBaseValues.get(source.getServer()).all();
        if (observed.isEmpty()) {
            send(source, "No base values observed yet.");
            return 0;
        }

        send(source, "Observed base values (" + observed.size() + " entity types):");
        observed.forEach((id, values) -> {
            String line = String.format("%s | HP %s | Armor %s | Atk %s", id,
                    number(values.maxHealth().orElse(null)), number(values.armor().orElse(null)), number(values.attackDamage().orElse(null)));
            Constants.LOGGER.info(line);
            send(source, line);
        });
        return observed.size();
    }

    private static int forget(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = ResourceLocationArgument.getId(context, "entity").toString();
        if (!ObservedBaseValues.get(source.getServer()).forget(id)) {
            send(source, "Nothing observed for " + id);
            return 0;
        }

        BossStats stats = StatsService.recalculateOne(source.getServer(), id);
        if (stats != null) {
            StatsService.broadcastUpdate(List.of(stats));
        }
        send(source, "Forgot the observed base values of " + id);
        return 1;
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static String format(BossStats s) {
        String level = !s.hasLevel() ? "-" : s.levelMin() == s.levelMax() ? String.valueOf(s.levelMin()) : s.levelMin() + "-" + s.levelMax();
        String approx = s.approximateMask() != 0 ? "~" : "";
        return String.format("%s | Lvl %s | HP %s%s | Armor %s | Atk %s | Base %s / %s / %s (%s) | %s | %s | DD %s",
                s.entityId(), level,
                approx, range(s.hpMin(), s.hpMax()), range(s.armorMin(), s.armorMax()), range(s.attackMin(), s.attackMax()),
                number(s.baseHp()), number(s.baseArmor()), number(s.baseAttack()), s.baseSource(),
                s.location().map(DebugCommand::location).orElse("no location"),
                s.zone().map(zone -> String.format("Zone %s %d (entity %d, loot %s)", zone.typeKey(), zone.level(), zone.entityLevel(),
                        zone.allowLoot() ? String.valueOf(zone.rewardLevel()) : "-")).orElse("no zone"),
                s.dungeonScaled() ? "applied" : "not applied");
    }

    private static String location(BossLocation location) {
        return "Dim " + location.dimension().map(Object::toString).orElse("?")
                + " / " + location.type().map(type -> type.getSerializedName() + location.id().map(id -> " " + id).orElse("")).orElse("?");
    }

    private static int compare(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        LivingEntity target = targetedEntity(player);
        if (target == null) {
            send(source, "No living entity in the crosshair (32 blocks).");
            return 0;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        BossStats predicted = ServerBossStatsCache.get().stream().filter(stats -> stats.entityId().equals(id)).findFirst().orElse(null);
        if (predicted == null) {
            send(source, id + ": no predicted stats (not a boss of Boss Checklist, hidden, or not calculated).");
            return 0;
        }

        send(source, "Compare " + id + " (predicted | actual | difference):");
        send(source, compareLine("HP", predicted.hpMin(), predicted.hpMax(), predicted.approximate(BossStats.APPROX_HEALTH), target.getAttributeValue(Attributes.MAX_HEALTH)));
        send(source, compareLine("Armor", predicted.armorMin(), predicted.armorMax(), predicted.approximate(BossStats.APPROX_ARMOR), target.getAttributeValue(Attributes.ARMOR)));
        if (target.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            send(source, compareLine("Attack", predicted.attackMin(), predicted.attackMax(), predicted.approximate(BossStats.APPROX_ATTACK), target.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        }
        if (!predicted.hasLevel() && hasDynamicDifficultyModifier(target)) {
            send(source, "Note: this entity got Dynamic Difficulty modifiers from its location, the prediction only knows levels from entity settings.");
        }
        return 1;
    }

    private static boolean hasDynamicDifficultyModifier(LivingEntity entity) {
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        return health != null && health.getModifiers().stream().anyMatch(modifier -> modifier.id().getNamespace().equals(Constants.DYNAMIC_DIFFICULTY_ID));
    }

    // The living entity whose (slightly enlarged) hitbox is hit first by the line of sight, ignoring blocks.
    private static LivingEntity targetedEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(MAX_DISTANCE));
        AABB area = new AABB(eye, end).inflate(1.0);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && !entity.isSpectator())) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            double distance = box.contains(eye) ? 0.0 : box.clip(eye, end).map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    // The difference is measured against the predicted range, so a value inside the range has no difference.
    private static String compareLine(String name, double min, double max, boolean approximate, double actual) {
        double difference = actual < min ? actual - min : actual > max ? actual - max : 0.0;
        double reference = actual < min ? min : actual > max ? max : actual;
        String percent = reference == 0.0 ? "" : String.format(Locale.ROOT, " (%+.1f%%)", difference / reference * 100.0);
        String line = String.format(Locale.ROOT, "%s: %s%s | %s | %+.1f%s", name, approximate ? "~" : "", range(min, max), number(actual), difference, percent);
        return difference == 0.0 ? line + " (within range)" : line;
    }
    private static String range(double min, double max) {
        return number(min).equals(number(max)) ? number(max) : number(min) + "-" + number(max);
    }

    private static String number(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "%.1f", value);
    }
}
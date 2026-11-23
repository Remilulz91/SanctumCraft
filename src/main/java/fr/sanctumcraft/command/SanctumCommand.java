package fr.sanctumcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.PortalRecord;
import fr.sanctumcraft.instance.SanctumInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

/**
 * /sanctum status            - everyone
 * /sanctum leave             - everyone (inside a sanctum: same as the exit portal)
 * /sanctum list              - OP
 * /sanctum debug ...         - OP, debug build only
 */
public final class SanctumCommand {

    private SanctumCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("sanctum")
                .then(CommandManager.literal("status").executes(SanctumCommand::status))
                .then(CommandManager.literal("leave").executes(SanctumCommand::leave))
                .then(CommandManager.literal("list").requires(s -> s.hasPermissionLevel(2)).executes(SanctumCommand::list));

        if (SanctumCraft.DEBUG_BUILD) {
            root.then(CommandManager.literal("debug").requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("generate").executes(SanctumCommand::debugGenerate))
                    .then(CommandManager.literal("close").executes(SanctumCommand::debugClose))
                    .then(CommandManager.literal("closeall").executes(SanctumCommand::debugCloseAll))
                    .then(CommandManager.literal("time")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0, 7200))
                                    .executes(ctx -> debugTime(ctx, IntegerArgumentType.getInteger(ctx, "seconds")))))
                    .then(CommandManager.literal("cooldown")
                            .then(CommandManager.literal("reset").executes(SanctumCommand::debugCooldownReset))));
        }
        dispatcher.register(root);
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return 0;
        MinecraftServer server = src.getServer();
        SanctumInstance inst = InstanceManager.instanceOf(player);
        if (inst != null) {
            long remaining = InstanceManager.remainingTicks(server, inst);
            src.sendFeedback(() -> Text.translatable("command.sanctumcraft.status.inside",
                    InstanceManager.formatTime(remaining), inst.members.size()).formatted(Formatting.LIGHT_PURPLE), false);
            return 1;
        }
        PortalRecord portal = InstanceManager.nearestPortal(player, 32);
        if (portal == null) {
            src.sendFeedback(() -> Text.translatable("command.sanctumcraft.status.no_portal").formatted(Formatting.GRAY), false);
            return 0;
        }
        long cd = InstanceManager.cooldownTicks(server, portal);
        if (portal.activeInstance != null) {
            src.sendFeedback(() -> Text.translatable("command.sanctumcraft.status.portal_active").formatted(Formatting.GREEN), false);
        } else if (cd > 0) {
            src.sendFeedback(() -> Text.translatable("command.sanctumcraft.status.portal_cooldown",
                    InstanceManager.formatTime(cd)).formatted(Formatting.GOLD), false);
        } else {
            src.sendFeedback(() -> Text.translatable("command.sanctumcraft.status.portal_ready").formatted(Formatting.GREEN), false);
        }
        return 1;
    }

    private static int leave(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        if (InstanceManager.instanceOf(player) == null) {
            ctx.getSource().sendError(Text.translatable("command.sanctumcraft.not_inside"));
            return 0;
        }
        InstanceManager.exitSanctum(player);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        MinecraftServer server = src.getServer();
        src.sendFeedback(() -> Text.translatable("command.sanctumcraft.list.header",
                InstanceManager.portals(server).size(), InstanceManager.instances(server).size()).formatted(Formatting.GOLD), false);
        for (SanctumInstance inst : InstanceManager.instances(server)) {
            String id = inst.id.toString().substring(0, 8);
            String time = inst.status == SanctumInstance.Status.ACTIVE
                    ? InstanceManager.formatTime(InstanceManager.remainingTicks(server, inst)) : "-";
            src.sendFeedback(() -> Text.literal(" - " + id + "  slot " + inst.slot + "  " + inst.status
                    + "  " + time + "  " + inst.members.size() + "p").formatted(Formatting.GRAY), false);
        }
        return 1;
    }

    private static int debugGenerate(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        if (InstanceManager.instanceOf(player) != null) {
            ctx.getSource().sendError(Text.translatable("command.sanctumcraft.already_inside"));
            return 0;
        }
        if (!InstanceManager.debugGenerate(player)) {
            ctx.getSource().sendError(Text.translatable("message.sanctumcraft.no_slot"));
            return 0;
        }
        return 1;
    }

    private static int debugClose(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        SanctumInstance inst = player != null ? InstanceManager.instanceOf(player) : null;
        if (inst == null) {
            ctx.getSource().sendError(Text.translatable("command.sanctumcraft.not_inside"));
            return 0;
        }
        InstanceManager.collapse(ctx.getSource().getServer(), inst);
        return 1;
    }

    private static int debugCloseAll(CommandContext<ServerCommandSource> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        int n = 0;
        for (SanctumInstance inst : new ArrayList<>(InstanceManager.instances(server))) {
            if (inst.status != SanctumInstance.Status.CLOSING) {
                InstanceManager.collapse(server, inst);
                n++;
            }
        }
        int count = n;
        ctx.getSource().sendFeedback(() -> Text.translatable("command.sanctumcraft.closed", count), true);
        return n;
    }

    private static int debugTime(CommandContext<ServerCommandSource> ctx, int seconds) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        SanctumInstance inst = player != null ? InstanceManager.instanceOf(player) : null;
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) {
            ctx.getSource().sendError(Text.translatable("command.sanctumcraft.not_inside"));
            return 0;
        }
        InstanceManager.setRemaining(ctx.getSource().getServer(), inst, seconds);
        ctx.getSource().sendFeedback(() -> Text.translatable("command.sanctumcraft.time_set",
                InstanceManager.formatTime(seconds * 20L)), true);
        return 1;
    }

    private static int debugCooldownReset(CommandContext<ServerCommandSource> ctx) {
        int n = InstanceManager.resetCooldowns(ctx.getSource().getServer());
        ctx.getSource().sendFeedback(() -> Text.translatable("command.sanctumcraft.cooldown_reset", n), true);
        return 1;
    }
}

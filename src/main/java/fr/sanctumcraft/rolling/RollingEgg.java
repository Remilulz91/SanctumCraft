package fr.sanctumcraft.rolling;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.SanctumEggItem;
import fr.sanctumcraft.hud.HudSync;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.net.SanctumNetwork;
import fr.sanctumcraft.net.SanctumNetwork.RollingPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * Rolling egg: inside a sanctum, a player can curl up into an egg and roll faster.
 * While rolling: no attacking, digging, opening or using anything; sneak = dash (with a cooldown).
 * Taking damage, dying, leaving the sanctum or pressing the key again stands the player back up.
 */
public final class RollingEgg {

    private static final Identifier SPEED_ID = SanctumCraft.id("rolling_egg_speed");

    private static final class State {
        long dashReadyTick;
        boolean wasSneaking;
    }

    private static final Map<UUID, State> ROLLING = new HashMap<>();

    /** Client side: is the local player rolling (set by the client from the HUD packet). */
    public static volatile boolean clientLocalRolling;
    /** Client side: every rolling player known by this client. */
    public static final Set<UUID> CLIENT_ROLLING = Collections.synchronizedSet(new HashSet<>());

    private RollingEgg() {}

    public static boolean isRolling(PlayerEntity player) {
        if (player.getWorld().isClient) return CLIENT_ROLLING.contains(player.getUuid());
        return ROLLING.containsKey(player.getUuid());
    }

    public static int dashCooldownTicks(ServerPlayerEntity player) {
        State s = ROLLING.get(player.getUuid());
        if (s == null || player.getServer() == null) return 0;
        return (int) Math.max(0, s.dashReadyTick - player.getServer().getTicks());
    }

    // ------------------------------------------------------------------ toggle

    public static void toggle(ServerPlayerEntity player) {
        if (ROLLING.containsKey(player.getUuid())) {
            stop(player, null);
            return;
        }
        SanctumInstance inst = InstanceManager.instanceOf(player);
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) {
            deny(player, "message.sanctumcraft.rolling.only_sanctum");
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.hasVehicle()) return;
        if (!SanctumConfig.rollingWithEggs && carriesEgg(player)) {
            deny(player, "message.sanctumcraft.rolling.egg");
            return;
        }
        State s = new State();
        s.wasSneaking = player.isSneaking();
        s.dashReadyTick = player.getServer().getTicks() + 10;
        ROLLING.put(player.getUuid(), s);
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
            speed.addTemporaryModifier(new EntityAttributeModifier(SPEED_ID, SanctumConfig.rollingSpeedPercent / 100.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TURTLE_EGG_CRACK,
                SoundCategory.PLAYERS, 0.8F, 1.4F);
        broadcast(player.getServer(), player.getUuid(), true);
        HudSync.refresh(player);
    }

    /** Stands the player back up. reasonKey = action bar message, or null. */
    public static void stop(ServerPlayerEntity player, String reasonKey) {
        if (ROLLING.remove(player.getUuid()) == null) return;
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        if (reasonKey != null) player.sendMessage(Text.translatable(reasonKey).formatted(Formatting.GOLD), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TURTLE_EGG_HATCH,
                SoundCategory.PLAYERS, 0.6F, 1.2F);
        if (player.getServer() != null) broadcast(player.getServer(), player.getUuid(), false);
        HudSync.refresh(player);
    }

    private static void deny(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
    }

    private static boolean carriesEgg(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).getItem() instanceof SanctumEggItem && !SanctumEggItem.isSecured(inv.getStack(i))) return true;
        }
        return false;
    }

    private static void broadcast(MinecraftServer server, UUID id, boolean rolling) {
        RollingPayload payload = new RollingPayload(id, rolling);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) SanctumNetwork.send(p, payload);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(MinecraftServer server) {
        if (ROLLING.isEmpty()) return;
        for (UUID id : new ArrayList<>(ROLLING.keySet())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null) {
                ROLLING.remove(id);
                broadcast(server, id, false);
                continue;
            }
            SanctumInstance inst = InstanceManager.instanceOf(p);
            if (!p.isAlive() || p.isSpectator() || p.hasVehicle() || inst == null
                    || inst.status != SanctumInstance.Status.ACTIVE) {
                stop(p, null);
                continue;
            }
            if (!SanctumConfig.rollingWithEggs && carriesEgg(p)) {
                stop(p, "message.sanctumcraft.rolling.egg");
                continue;
            }
            State s = ROLLING.get(id);
            boolean sneaking = p.isSneaking();
            if (sneaking && !s.wasSneaking) {
                if (server.getTicks() >= s.dashReadyTick) dash(p, s, server);
            }
            s.wasSneaking = sneaking;
            if (server.getTicks() % 4 == 0 && p.getVelocity().horizontalLengthSquared() > 0.005
                    && p.getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 1, 0.15, 0.02, 0.15, 0.0);
            }
        }
    }

    private static void dash(ServerPlayerEntity p, State s, MinecraftServer server) {
        Vec3d dir = Vec3d.fromPolar(0, p.getYaw()).normalize();
        double power = 1.1;
        p.addVelocity(dir.x * power, p.isOnGround() ? 0.2 : 0.0, dir.z * power);
        p.velocityModified = true;
        s.dashReadyTick = server.getTicks() + SanctumConfig.rollingDashCooldownSeconds * 20L;
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.7F, 1.3F);
        if (p.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        }
    }

    // ------------------------------------------------------------------ events

    public static void onJoin(ServerPlayerEntity player) {
        for (UUID id : ROLLING.keySet()) SanctumNetwork.send(player, new RollingPayload(id, true));
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        if (ROLLING.remove(player.getUuid()) != null) {
            EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(SPEED_ID);
            if (player.getServer() != null) {
                RollingPayload payload = new RollingPayload(player.getUuid(), false);
                for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) {
                    if (p != player) SanctumNetwork.send(p, payload);
                }
            }
        }
    }

    private static boolean blocked(PlayerEntity player, World world) {
        boolean rolling = world.isClient ? clientLocalRolling && player.isMainPlayer() : ROLLING.containsKey(player.getUuid());
        if (rolling && world.isClient) {
            player.sendMessage(Text.translatable("message.sanctumcraft.rolling.busy").formatted(Formatting.GOLD), true);
        }
        return rolling;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                blocked(player, world) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
                blocked(player, world) ? ActionResult.FAIL : ActionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                blocked(player, world) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                blocked(player, world) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                blocked(player, world) ? TypedActionResult.fail(player.getStackInHand(hand))
                        : TypedActionResult.pass(player.getStackInHand(hand)));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0 && entity instanceof ServerPlayerEntity p && ROLLING.containsKey(p.getUuid())) {
                stop(p, "message.sanctumcraft.rolling.hit");
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) stop(p, null);
        });
    }
}

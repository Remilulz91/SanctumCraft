package fr.sanctumcraft.instance;

import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.registry.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Optional "keep inventory, but only in the sanctum" (config: sanctumKeepInventory).
 * The death drop is cancelled by PlayerEntityMixin; the inventory and XP are then copied
 * to the respawned player here, like the vanilla keepInventory gamerule does.
 */
public final class KeepInventoryHandler {

    private static final Set<UUID> KEPT = new HashSet<>();

    private KeepInventoryHandler() {}

    public static void register() {
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive || !KEPT.remove(oldPlayer.getUuid())) return;
            if (oldPlayer.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return; // vanilla already did it
            newPlayer.getInventory().clone(oldPlayer.getInventory());
            newPlayer.experienceLevel = oldPlayer.experienceLevel;
            newPlayer.totalExperience = oldPlayer.totalExperience;
            newPlayer.experienceProgress = oldPlayer.experienceProgress;
            newPlayer.setScore(oldPlayer.getScore());
        });
    }

    /** True if this dying player must keep everything. Remembers the decision for the respawn copy. */
    public static boolean shouldKeep(PlayerEntity player) {
        if (!SanctumConfig.sanctumKeepInventory) return false;
        if (!(player instanceof ServerPlayerEntity) || !ModDimensions.isSanctum(player.getWorld())) return false;
        KEPT.add(player.getUuid());
        return true;
    }
}

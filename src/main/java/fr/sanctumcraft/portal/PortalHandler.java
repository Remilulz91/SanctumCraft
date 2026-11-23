package fr.sanctumcraft.portal;

import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.registry.ModDimensions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Debounces portal contact: a portal only triggers on a FRESH contact
 * (player was not touching a portal during the previous ticks). This prevents
 * ping-ponging when a player arrives inside a portal after a teleport.
 */
public final class PortalHandler {

    private static final int CONTACT_GRACE_TICKS = 3;
    private static final Map<UUID, Integer> LAST_CONTACT = new HashMap<>();

    private PortalHandler() {}

    public static void onPlayerInPortal(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        int now = world.getServer().getTicks();
        Integer last = LAST_CONTACT.put(player.getUuid(), now);
        if (last != null && now - last <= CONTACT_GRACE_TICKS) {
            return; // still standing in a portal
        }
        if (player.isSpectator()) {
            return;
        }
        if (ModDimensions.isSanctum(world)) {
            InstanceManager.exitSanctum(player);
        } else {
            InstanceManager.enterPortal(player, world, pos);
        }
    }

    /** Marks the player as touching a portal right now (call after a teleport). */
    public static void markContact(ServerPlayerEntity player) {
        LAST_CONTACT.put(player.getUuid(), player.getServer() != null ? player.getServer().getTicks() : 0);
    }

    public static void forget(UUID uuid) {
        LAST_CONTACT.remove(uuid);
    }
}

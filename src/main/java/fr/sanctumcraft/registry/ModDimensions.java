package fr.sanctumcraft.registry;

import fr.sanctumcraft.SanctumCraft;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/** The sanctum dimension is declared in data/sanctumcraft/dimension/sanctum.json. */
public final class ModDimensions {

    public static final RegistryKey<World> SANCTUM =
            RegistryKey.of(RegistryKeys.WORLD, SanctumCraft.id("sanctum"));

    private ModDimensions() {}

    public static boolean isSanctum(WorldAccess world) {
        return world instanceof World w && w.getRegistryKey() == SANCTUM;
    }
}

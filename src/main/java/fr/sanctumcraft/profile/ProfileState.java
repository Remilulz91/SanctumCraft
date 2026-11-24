package fr.sanctumcraft.profile;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProfileState extends PersistentState {

    private static final Type<ProfileState> TYPE = new Type<>(ProfileState::new, ProfileState::fromNbt, null);

    final Map<UUID, HunterProfile> profiles = new HashMap<>();

    public static ProfileState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "sanctumcraft_profiles");
    }

    public HunterProfile of(UUID uuid) {
        return profiles.computeIfAbsent(uuid, u -> {
            markDirty();
            return new HunterProfile();
        });
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound all = new NbtCompound();
        profiles.forEach((uuid, p) -> all.put(uuid.toString(), p.toNbt()));
        nbt.put("profiles", all);
        return nbt;
    }

    private static ProfileState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ProfileState s = new ProfileState();
        NbtCompound all = nbt.getCompound("profiles");
        for (String key : all.getKeys()) {
            try {
                s.profiles.put(UUID.fromString(key), HunterProfile.fromNbt(all.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return s;
    }
}

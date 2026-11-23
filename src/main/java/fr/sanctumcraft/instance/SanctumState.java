package fr.sanctumcraft.instance;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.*;

/** World-saved data: portals and sanctums. Stored in the overworld's data folder. */
public final class SanctumState extends PersistentState {

    private static final String KEY = "sanctumcraft";

    private static final Type<SanctumState> TYPE = new Type<>(SanctumState::new, SanctumState::fromNbt, null);

    public final Map<UUID, PortalRecord> portals = new LinkedHashMap<>();
    public final Map<UUID, SanctumInstance> instances = new LinkedHashMap<>();

    public static SanctumState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    public PortalRecord findPortal(String dimension, BlockPos pos) {
        for (PortalRecord r : portals.values()) {
            if (r.contains(dimension, pos)) return r;
        }
        return null;
    }

    public SanctumInstance findInstanceAt(BlockPos pos) {
        for (SanctumInstance i : instances.values()) {
            if (i.contains(pos)) return i;
        }
        return null;
    }

    /** Lowest free slot, or -1. */
    public int freeSlot(int max) {
        Set<Integer> used = new HashSet<>();
        for (SanctumInstance i : instances.values()) used.add(i.slot);
        for (int s = 0; s < max; s++) {
            if (!used.contains(s)) return s;
        }
        return -1;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList p = new NbtList();
        for (PortalRecord r : portals.values()) p.add(r.toNbt());
        nbt.put("portals", p);
        NbtList i = new NbtList();
        for (SanctumInstance inst : instances.values()) i.add(inst.toNbt());
        nbt.put("instances", i);
        return nbt;
    }

    private static SanctumState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        SanctumState state = new SanctumState();
        NbtList p = nbt.getList("portals", NbtElement.COMPOUND_TYPE);
        for (int k = 0; k < p.size(); k++) {
            PortalRecord r = PortalRecord.fromNbt(p.getCompound(k));
            state.portals.put(r.id, r);
        }
        NbtList i = nbt.getList("instances", NbtElement.COMPOUND_TYPE);
        for (int k = 0; k < i.size(); k++) {
            SanctumInstance inst = SanctumInstance.fromNbt(i.getCompound(k));
            state.instances.put(inst.id, inst);
        }
        return state;
    }
}

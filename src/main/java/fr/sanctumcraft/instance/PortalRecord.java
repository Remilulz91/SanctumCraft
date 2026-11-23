package fr.sanctumcraft.instance;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** One lit portal in a normal dimension. Each portal has its own cooldown. */
public final class PortalRecord {

    public final UUID id;
    public final String dimension;
    public final BlockBox box;
    /** Overworld game time (ticks) at which the portal can be used again. */
    public long cooldownUntil;
    /** Sanctum currently linked to this portal, or null. */
    public UUID activeInstance;

    public PortalRecord(UUID id, String dimension, BlockBox box) {
        this.id = id;
        this.dimension = dimension;
        this.box = box;
    }

    public boolean contains(String dim, BlockPos pos) {
        return dimension.equals(dim) && box.contains(pos);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("id", id);
        tag.putString("dim", dimension);
        tag.putIntArray("box", new int[]{box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ()});
        tag.putLong("cooldownUntil", cooldownUntil);
        if (activeInstance != null) tag.putUuid("active", activeInstance);
        return tag;
    }

    public static PortalRecord fromNbt(NbtCompound tag) {
        int[] b = tag.getIntArray("box");
        BlockBox box = new BlockBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        PortalRecord r = new PortalRecord(tag.getUuid("id"), tag.getString("dim"), box);
        r.cooldownUntil = tag.getLong("cooldownUntil");
        if (tag.containsUuid("active")) r.activeInstance = tag.getUuid("active");
        return r;
    }
}

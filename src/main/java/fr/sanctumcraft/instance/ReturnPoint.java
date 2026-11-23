package fr.sanctumcraft.instance;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/** Where a player is sent back when leaving a sanctum. */
public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {

    public static ReturnPoint of(ServerPlayerEntity player) {
        return new ReturnPoint(player.getWorld().getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("dim", dimension);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static ReturnPoint fromNbt(NbtCompound tag) {
        return new ReturnPoint(tag.getString("dim"), tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"));
    }
}

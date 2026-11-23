package fr.sanctumcraft.instance;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;

/**
 * A room of the layout. {@code cx/cz} are grid cells; {@code doors} are the sides
 * connected to a neighbour (the others get sealed after placement).
 */
public record PlacedRoom(int cx, int cz, RoomKind kind, String template, EnumSet<Direction> doors) {

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("cx", cx);
        tag.putInt("cz", cz);
        tag.putString("kind", kind.name());
        tag.putString("template", template);
        int mask = 0;
        for (Direction d : doors) mask |= 1 << d.getHorizontal();
        tag.putInt("doors", mask);
        return tag;
    }

    public static PlacedRoom fromNbt(NbtCompound tag) {
        EnumSet<Direction> doors = EnumSet.noneOf(Direction.class);
        int mask = tag.getInt("doors");
        for (Direction d : Direction.Type.HORIZONTAL) {
            if ((mask & (1 << d.getHorizontal())) != 0) doors.add(d);
        }
        RoomKind kind;
        try {
            kind = RoomKind.valueOf(tag.getString("kind"));
        } catch (IllegalArgumentException e) {
            kind = RoomKind.HALL;
        }
        return new PlacedRoom(tag.getInt("cx"), tag.getInt("cz"), kind, tag.getString("template"), doors);
    }
}

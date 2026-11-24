package fr.sanctumcraft.instance;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

/**
 * A room of the layout, covering {@code w x h} grid cells from cell {@code (cx, cz)}.
 * Doors are stored per edge cell: see {@link #doorCode(int, int, Direction)}.
 */
public record PlacedRoom(int cx, int cz, int w, int h, RoomKind kind, String template, Set<Integer> doors,
                         boolean nestActive) {

    /** Encodes "door on side {@code dir} of local cell (lx, lz)". */
    public static int doorCode(int lx, int lz, Direction dir) {
        return ((lx * 4) + lz) * 4 + dir.getHorizontal();
    }

    public boolean hasDoor(int lx, int lz, Direction dir) {
        return doors.contains(doorCode(lx, lz, dir));
    }

    public boolean contains(int x, int z) {
        return x >= cx && x < cx + w && z >= cz && z < cz + h;
    }

    public int doorCount() {
        return doors.size();
    }

    public PlacedRoom withKind(RoomKind newKind, String newTemplate, boolean active) {
        return new PlacedRoom(cx, cz, w, h, newKind, newTemplate, doors, active);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("cx", cx);
        tag.putInt("cz", cz);
        tag.putInt("w", w);
        tag.putInt("h", h);
        tag.putString("kind", kind.name());
        tag.putString("template", template);
        tag.putIntArray("doorList", doors.stream().mapToInt(Integer::intValue).toArray());
        tag.putBoolean("nestActive", nestActive);
        return tag;
    }

    public static PlacedRoom fromNbt(NbtCompound tag) {
        Set<Integer> doors = new HashSet<>();
        if (tag.contains("doorList")) {
            for (int d : tag.getIntArray("doorList")) doors.add(d);
        } else {
            // 0.1 format: bit mask of horizontal directions on a 1x1 room
            int mask = tag.getInt("doors");
            for (Direction d : Direction.Type.HORIZONTAL) {
                if ((mask & (1 << d.getHorizontal())) != 0) doors.add(doorCode(0, 0, d));
            }
        }
        RoomKind kind;
        try {
            kind = RoomKind.valueOf(tag.getString("kind"));
        } catch (IllegalArgumentException e) {
            kind = RoomKind.HALL;
        }
        int w = tag.contains("w") ? tag.getInt("w") : 1;
        int h = tag.contains("h") ? tag.getInt("h") : 1;
        return new PlacedRoom(tag.getInt("cx"), tag.getInt("cz"), w, h, kind, tag.getString("template"), doors,
                tag.getBoolean("nestActive"));
    }
}

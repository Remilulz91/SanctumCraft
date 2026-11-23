package fr.sanctumcraft.instance;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/** One generated sanctum, living in its own slot of the sanctum dimension. */
public final class SanctumInstance {

    public enum Status { GENERATING, ACTIVE, CLOSING }

    public final UUID id;
    /** Portal that opened it, or null for debug sanctums. */
    public final UUID portalId;
    public final int slot;
    public final BlockPos origin;
    public final List<PlacedRoom> rooms;
    public Status status;
    /** Overworld game time at which the sanctum collapses. */
    public long endTick;
    public BlockPos spawn;
    public float spawnYaw;
    /** Every player that entered this sanctum at least once (for the player cap). */
    public final Set<UUID> members = new LinkedHashSet<>();
    public final Map<UUID, ReturnPoint> returns = new HashMap<>();

    // ---- transient (not saved) ----
    /** Players frozen in front of the portal while the sanctum generates. */
    public final Map<UUID, Vec3d> waiting = new LinkedHashMap<>();
    public int roomsPlaced;
    public long generationStartTick;
    public ServerBossBar bossBar;
    public int clearRoom;
    public int clearIndex;
    public boolean entitiesCleared;
    public long lastWarnSecond = -1;

    public SanctumInstance(UUID id, UUID portalId, int slot, BlockPos origin, List<PlacedRoom> rooms) {
        this.id = id;
        this.portalId = portalId;
        this.slot = slot;
        this.origin = origin;
        this.rooms = rooms;
        this.status = Status.GENERATING;
    }

    public BlockPos roomOrigin(PlacedRoom room) {
        return origin.add(room.cx() * RoomLayout.CELL, 0, room.cz() * RoomLayout.CELL);
    }

    /** Whole area reserved by this sanctum (used to know who is inside). */
    public BlockBox bounds() {
        int size = RoomLayout.GRID * RoomLayout.CELL;
        return new BlockBox(origin.getX() - 2, origin.getY() - 64, origin.getZ() - 2,
                origin.getX() + size + 1, origin.getY() + RoomLayout.ROOM_HEIGHT + 32, origin.getZ() + size + 1);
    }

    public boolean contains(BlockPos pos) {
        return bounds().contains(pos);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("id", id);
        if (portalId != null) tag.putUuid("portal", portalId);
        tag.putInt("slot", slot);
        tag.putIntArray("origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
        // A sanctum that was still generating when the server stopped is closed on restart.
        tag.putString("status", status.name());
        tag.putLong("endTick", endTick);
        if (spawn != null) tag.putIntArray("spawn", new int[]{spawn.getX(), spawn.getY(), spawn.getZ()});
        tag.putFloat("spawnYaw", spawnYaw);
        NbtList roomList = new NbtList();
        for (PlacedRoom r : rooms) roomList.add(r.toNbt());
        tag.put("rooms", roomList);
        NbtList memberList = new NbtList();
        for (UUID m : members) {
            NbtCompound e = new NbtCompound();
            e.putUuid("uuid", m);
            ReturnPoint rp = returns.get(m);
            if (rp != null) e.put("return", rp.toNbt());
            memberList.add(e);
        }
        tag.put("members", memberList);
        return tag;
    }

    public static SanctumInstance fromNbt(NbtCompound tag) {
        int[] o = tag.getIntArray("origin");
        List<PlacedRoom> rooms = new ArrayList<>();
        NbtList roomList = tag.getList("rooms", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < roomList.size(); i++) rooms.add(PlacedRoom.fromNbt(roomList.getCompound(i)));
        SanctumInstance inst = new SanctumInstance(tag.getUuid("id"),
                tag.containsUuid("portal") ? tag.getUuid("portal") : null,
                tag.getInt("slot"), new BlockPos(o[0], o[1], o[2]), rooms);
        try {
            inst.status = Status.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException e) {
            inst.status = Status.CLOSING;
        }
        inst.endTick = tag.getLong("endTick");
        if (tag.contains("spawn")) {
            int[] s = tag.getIntArray("spawn");
            inst.spawn = new BlockPos(s[0], s[1], s[2]);
        }
        inst.spawnYaw = tag.getFloat("spawnYaw");
        NbtList memberList = tag.getList("members", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < memberList.size(); i++) {
            NbtCompound e = memberList.getCompound(i);
            UUID uuid = e.getUuid("uuid");
            inst.members.add(uuid);
            if (e.contains("return")) inst.returns.put(uuid, ReturnPoint.fromNbt(e.getCompound("return")));
        }
        return inst;
    }
}

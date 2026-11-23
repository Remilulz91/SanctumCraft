package fr.sanctumcraft.instance;

import fr.sanctumcraft.config.SanctumConfig;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 * Builds the room graph of a sanctum on a square grid.
 *
 * <p>Rooms are fixed templates of {@link #CELL}x{@link #CELL} blocks with a 3-wide doorway
 * in the middle of each side. The layout is a random tree grown from the entrance
 * (north edge), plus a few extra doors that create loops. Then roles are assigned:
 * destination = farthest room, nests = next farthest rooms, the rest = halls.</p>
 */
public final class RoomLayout {

    /** Size of one grid cell / room footprint, in blocks. */
    public static final int CELL = 25;
    /** Height of a room template, in blocks. */
    public static final int ROOM_HEIGHT = 10;
    /** Grid side, in cells. */
    public static final int GRID = 8;

    public static final String T_ENTRANCE = "room/entrance";
    public static final String T_NEST = "room/nest";
    public static final String T_DESTINATION = "room/destination";
    public static final String[] T_HALLS = {"room/hall_pillars", "room/hall_crystals", "room/hall_ruins"};

    private static final Direction[] DIRS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private RoomLayout() {}

    public static List<PlacedRoom> generate(Random random) {
        int target = Math.min(SanctumConfig.roomsPerSanctum, GRID * GRID);
        int startX = GRID / 2;
        int startZ = 0;

        Map<Long, EnumSet<Direction>> doors = new LinkedHashMap<>();
        List<int[]> cells = new ArrayList<>();
        doors.put(key(startX, startZ), EnumSet.noneOf(Direction.class));
        cells.add(new int[]{startX, startZ});

        // Grow a random tree. Prefer extending recent cells to get corridors rather than blobs.
        int attempts = 0;
        while (cells.size() < target && attempts++ < 5000) {
            int idx = random.nextInt(4) == 0
                    ? random.nextInt(cells.size())
                    : Math.max(0, cells.size() - 1 - random.nextInt(Math.min(3, cells.size())));
            int[] from = cells.get(idx);
            Direction dir = DIRS[random.nextInt(DIRS.length)];
            int nx = from[0] + dir.getOffsetX();
            int nz = from[1] + dir.getOffsetZ();
            if (nx < 0 || nz < 0 || nx >= GRID || nz >= GRID) continue;
            if (doors.containsKey(key(nx, nz))) continue;
            doors.get(key(from[0], from[1])).add(dir);
            EnumSet<Direction> n = EnumSet.noneOf(Direction.class);
            n.add(dir.getOpposite());
            doors.put(key(nx, nz), n);
            cells.add(new int[]{nx, nz});
        }

        // Extra doors -> loops (never on the entrance, so it keeps a single way in).
        for (int[] c : cells) {
            if (c[0] == startX && c[1] == startZ) continue;
            for (Direction dir : new Direction[]{Direction.SOUTH, Direction.EAST}) {
                int nx = c[0] + dir.getOffsetX();
                int nz = c[1] + dir.getOffsetZ();
                if (nx == startX && nz == startZ) continue;
                EnumSet<Direction> other = doors.get(key(nx, nz));
                EnumSet<Direction> mine = doors.get(key(c[0], c[1]));
                if (other != null && !mine.contains(dir) && random.nextInt(100) < SanctumConfig.loopChancePercent) {
                    mine.add(dir);
                    other.add(dir.getOpposite());
                }
            }
        }

        // BFS distances from the entrance.
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        dist.put(key(startX, startZ), 0);
        queue.add(new int[]{startX, startZ});
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int d = dist.get(key(c[0], c[1]));
            for (Direction dir : doors.get(key(c[0], c[1]))) {
                int nx = c[0] + dir.getOffsetX();
                int nz = c[1] + dir.getOffsetZ();
                if (!dist.containsKey(key(nx, nz))) {
                    dist.put(key(nx, nz), d + 1);
                    queue.add(new int[]{nx, nz});
                }
            }
        }

        // Order cells by distance (farthest first), random tie-break.
        List<int[]> ordered = new ArrayList<>(cells);
        ordered.removeIf(c -> c[0] == startX && c[1] == startZ);
        Collections.shuffle(ordered, new java.util.Random(random.nextLong()));
        ordered.sort((a, b) -> Integer.compare(dist.getOrDefault(key(b[0], b[1]), 0),
                dist.getOrDefault(key(a[0], a[1]), 0)));

        Map<Long, RoomKind> kinds = new HashMap<>();
        kinds.put(key(startX, startZ), RoomKind.ENTRANCE);
        int nests = 0;
        for (int i = 0; i < ordered.size(); i++) {
            int[] c = ordered.get(i);
            RoomKind k;
            if (i == 0) {
                k = RoomKind.DESTINATION;
            } else if (nests < SanctumConfig.nestRooms && i % 2 == 1) {
                // Every other far room becomes a nest, so nests are spread out.
                k = RoomKind.NEST;
                nests++;
            } else {
                k = RoomKind.HALL;
            }
            kinds.put(key(c[0], c[1]), k);
        }

        List<PlacedRoom> rooms = new ArrayList<>();
        for (int[] c : cells) {
            RoomKind k = kinds.get(key(c[0], c[1]));
            String template = switch (k) {
                case ENTRANCE -> T_ENTRANCE;
                case NEST -> T_NEST;
                case DESTINATION -> T_DESTINATION;
                case HALL -> T_HALLS[random.nextInt(T_HALLS.length)];
            };
            rooms.add(new PlacedRoom(c[0], c[1], k, template, doors.get(key(c[0], c[1]))));
        }
        return rooms;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}

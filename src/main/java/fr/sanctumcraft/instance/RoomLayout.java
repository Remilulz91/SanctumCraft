package fr.sanctumcraft.instance;

import fr.sanctumcraft.config.SanctumConfig;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 * Builds the room graph of a sanctum on a square grid of {@link #CELL}-block cells.
 *
 * <ol>
 *   <li>Grow a random tree of rooms from the entrance (north edge). A new room is 1x1,
 *       or sometimes a 2x2 great hall.</li>
 *   <li>Add a few extra doors between neighbouring rooms (loops).</li>
 *   <li>Assign roles by distance from the entrance: destination = farthest room,
 *       nest candidates = next far rooms, of which only some are active.</li>
 *   <li>Shape the rest: straight/corner passages become corridors, dead ends often
 *       become small rooms, the others are halls.</li>
 * </ol>
 * Every doorway is 3 wide (cell offset 11..13) and 4 tall (y 1..4), in the middle of a cell side.
 */
public final class RoomLayout {

    public static final int CELL = 25;
    /** Height reserved for every room (cleared on collapse). */
    public static final int ROOM_HEIGHT = 14;
    public static final int GRID = 10;

    public static final String T_ENTRANCE = "room/entrance";
    public static final String T_NEST = "room/nest";
    public static final String T_DESTINATION = "room/destination";
    public static final String[] T_HALLS = {"room/hall_pillars", "room/hall_crystals", "room/hall_ruins"};
    public static final String[] T_SMALL = {"room/small_shrine", "room/small_storage"};
    public static final String[] T_GREAT = {"room/great_hall_columns", "room/great_hall_crypt"};
    public static final String T_CORRIDOR = "corridor";
    public static final String T_VAULT = "room/vault";

    private static final Direction[] DIRS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final int MAX_GREAT_HALLS = 2;

    private RoomLayout() {}

    /** Mutable node used during generation. */
    private static final class Node {
        final int cx, cz, w, h;
        final Set<Integer> doors = new HashSet<>();
        final Set<Node> links = new HashSet<>();
        RoomKind kind = RoomKind.HALL;
        boolean nestActive;

        Node(int cx, int cz, int w, int h) {
            this.cx = cx;
            this.cz = cz;
            this.w = w;
            this.h = h;
        }

        boolean contains(int x, int z) {
            return x >= cx && x < cx + w && z >= cz && z < cz + h;
        }
    }

    public static List<PlacedRoom> generate(Random random, SanctumDifficulty difficulty) {
        int target = Math.max(6, SanctumConfig.roomsPerSanctum);
        Node[][] grid = new Node[GRID][GRID];
        List<Node> nodes = new ArrayList<>();

        Node entrance = new Node(GRID / 2, 0, 1, 1);
        entrance.kind = RoomKind.ENTRANCE;
        put(grid, entrance);
        nodes.add(entrance);
        int great = 0;

        // 1. Random tree.
        int attempts = 0;
        while (nodes.size() < target && attempts++ < 8000) {
            Node from = nodes.get(random.nextInt(4) == 0
                    ? random.nextInt(nodes.size())
                    : Math.max(0, nodes.size() - 1 - random.nextInt(Math.min(3, nodes.size()))));
            int lx = random.nextInt(from.w);
            int lz = random.nextInt(from.h);
            Direction dir = DIRS[random.nextInt(DIRS.length)];
            int px = from.cx + lx, pz = from.cz + lz;
            int nx = px + dir.getOffsetX(), nz = pz + dir.getOffsetZ();
            if (!inGrid(nx, nz) || grid[nx][nz] != null) continue;
            if (from.contains(nx, nz)) continue;
            if (from == entrance && dir == Direction.NORTH) continue;

            Node next = null;
            if (great < MAX_GREAT_HALLS && nodes.size() > 2 && random.nextInt(100) < 18) {
                next = tryGreatHall(grid, nx, nz, dir, random);
                if (next != null) great++;
            }
            if (next == null) next = new Node(nx, nz, 1, 1);
            put(grid, next);
            nodes.add(next);
            link(from, px - from.cx, pz - from.cz, dir, next, nx - next.cx, nz - next.cz);
        }

        // 2. Loops.
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                Node a = grid[x][z];
                if (a == null || a == entrance) continue;
                for (Direction dir : new Direction[]{Direction.SOUTH, Direction.EAST}) {
                    int nx = x + dir.getOffsetX(), nz = z + dir.getOffsetZ();
                    if (!inGrid(nx, nz)) continue;
                    Node b = grid[nx][nz];
                    if (b == null || b == a || b == entrance || a.links.contains(b)) continue;
                    if (random.nextInt(100) < SanctumConfig.loopChancePercent) {
                        link(a, x - a.cx, z - a.cz, dir, b, nx - b.cx, nz - b.cz);
                    }
                }
            }
        }

        // 3. Distances from the entrance.
        Map<Node, Integer> dist = new HashMap<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        dist.put(entrance, 0);
        queue.add(entrance);
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            for (Node m : n.links) {
                if (!dist.containsKey(m)) {
                    dist.put(m, dist.get(n) + 1);
                    queue.add(m);
                }
            }
        }

        // 4. Roles: only 1x1 rooms can be destination / nests.
        List<Node> singles = new ArrayList<>();
        for (Node n : nodes) {
            if (n != entrance && n.w == 1 && n.h == 1) singles.add(n);
            if (n.w > 1) n.kind = RoomKind.GREAT_HALL;
        }
        Collections.shuffle(singles, new java.util.Random(random.nextLong()));
        singles.sort((a, b) -> Integer.compare(dist.getOrDefault(b, 0), dist.getOrDefault(a, 0)));

        if (!singles.isEmpty()) singles.get(0).kind = RoomKind.DESTINATION;
        List<Node> nests = new ArrayList<>();
        for (int i = 1; i < singles.size() && nests.size() < SanctumConfig.nestCandidates; i++) {
            // Take every other far room so that nests are spread around the map.
            if (i % 2 == 1 || singles.size() - i <= SanctumConfig.nestCandidates - nests.size()) {
                singles.get(i).kind = RoomKind.NEST;
                nests.add(singles.get(i));
            }
        }
        int minActive = Math.min(SanctumConfig.activeNestsMin, nests.size());
        int maxActive = Math.min(Math.max(SanctumConfig.activeNestsMax, minActive), nests.size());
        int active = minActive + (maxActive > minActive ? random.nextInt(maxActive - minActive + 1) : 0);
        Collections.shuffle(nests, new java.util.Random(random.nextLong()));
        for (int i = 0; i < active; i++) nests.get(i).nestActive = true;

        // 5. Shapes for the remaining 1x1 rooms.
        for (Node n : singles) {
            if (n.kind != RoomKind.HALL) continue;
            int doors = n.doors.size();
            if (doors == 1) {
                n.kind = random.nextInt(100) < 60 ? RoomKind.SMALL : RoomKind.HALL;
            } else if (doors == 2 || doors == 3) {
                n.kind = random.nextInt(100) < 65 ? RoomKind.CORRIDOR : RoomKind.HALL;
            }
        }

        // 6. Vaults: dead ends (exactly one door) among small rooms first, then halls.
        List<Node> deadEnds = new ArrayList<>();
        for (Node n : singles) {
            if (n.doors.size() == 1 && (n.kind == RoomKind.SMALL || n.kind == RoomKind.HALL)) deadEnds.add(n);
        }
        Collections.shuffle(deadEnds, new java.util.Random(random.nextLong()));
        deadEnds.sort((a, b) -> Boolean.compare(b.kind == RoomKind.SMALL, a.kind == RoomKind.SMALL));
        for (int i = 0; i < Math.min(SanctumConfig.vaultRooms, deadEnds.size()); i++) {
            deadEnds.get(i).kind = RoomKind.VAULT;
        }

        List<PlacedRoom> rooms = new ArrayList<>();
        for (Node n : nodes) {
            String template = switch (n.kind) {
                case ENTRANCE -> T_ENTRANCE;
                case NEST -> T_NEST;
                case DESTINATION -> T_DESTINATION;
                case HALL -> T_HALLS[random.nextInt(T_HALLS.length)];
                case SMALL -> T_SMALL[random.nextInt(T_SMALL.length)];
                case GREAT_HALL -> T_GREAT[random.nextInt(T_GREAT.length)];
                case CORRIDOR -> T_CORRIDOR;
                case VAULT -> T_VAULT;
            };
            rooms.add(new PlacedRoom(n.cx, n.cz, n.w, n.h, n.kind, template, n.doors, n.nestActive));
        }
        return rooms;
    }

    /** Tries to place a 2x2 hall containing cell (nx, nz), entered from direction {@code dir}. */
    private static Node tryGreatHall(Node[][] grid, int nx, int nz, Direction dir, Random random) {
        int x0, z0;
        if (dir.getAxis() == Direction.Axis.Z) {
            z0 = dir == Direction.SOUTH ? nz : nz - 1;
            x0 = random.nextBoolean() ? nx : nx - 1;
        } else {
            x0 = dir == Direction.EAST ? nx : nx - 1;
            z0 = random.nextBoolean() ? nz : nz - 1;
        }
        // The entrance cell row (z = 0) stays free around the entrance.
        if (z0 < 1) return null;
        for (int x = x0; x < x0 + 2; x++) {
            for (int z = z0; z < z0 + 2; z++) {
                if (!inGrid(x, z) || grid[x][z] != null) return null;
            }
        }
        return new Node(x0, z0, 2, 2);
    }

    private static void link(Node a, int alx, int alz, Direction dir, Node b, int blx, int blz) {
        a.doors.add(PlacedRoom.doorCode(alx, alz, dir));
        b.doors.add(PlacedRoom.doorCode(blx, blz, dir.getOpposite()));
        a.links.add(b);
        b.links.add(a);
    }

    private static void put(Node[][] grid, Node n) {
        for (int x = n.cx; x < n.cx + n.w; x++) {
            for (int z = n.cz; z < n.cz + n.h; z++) grid[x][z] = n;
        }
    }

    private static boolean inGrid(int x, int z) {
        return x >= 0 && z >= 0 && x < GRID && z < GRID;
    }
}

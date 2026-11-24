package fr.sanctumcraft.instance;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Corridors are built by code (no template) so that they only go toward their doors:
 * a 3x3 junction in the middle of the cell (x/z 11..13) plus one 3-wide, 4-tall arm per door.
 */
public final class CorridorBuilder {

    private static final BlockState FLOOR = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState FLOOR_CRACKED = Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState();
    private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.getDefaultState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
    private static final BlockState CEILING = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState LANTERN = Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
    private static final int FLAGS = Block.NOTIFY_LISTENERS;

    private CorridorBuilder() {}

    public static void build(ServerWorld world, BlockPos base, PlacedRoom room) {
        Random random = world.getRandom();
        // Junction 10..14 (walls on the ring, interior 11..13).
        box(world, base, random, 10, 10, 14, 14);
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (room.hasDoor(0, 0, dir)) {
                switch (dir) {
                    case NORTH -> box(world, base, random, 10, 0, 14, 10);
                    case SOUTH -> box(world, base, random, 10, 14, 14, RoomLayout.CELL - 1);
                    case WEST -> box(world, base, random, 0, 10, 10, 14);
                    default -> box(world, base, random, 14, 10, RoomLayout.CELL - 1, 14);
                }
            }
        }
        // Open the junction toward each arm.
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (!room.hasDoor(0, 0, dir)) continue;
            for (int w = 11; w <= 13; w++) {
                for (int y = 1; y <= 4; y++) {
                    BlockPos p = switch (dir) {
                        case NORTH -> base.add(w, y, 10);
                        case SOUTH -> base.add(w, y, 14);
                        case WEST -> base.add(10, y, w);
                        default -> base.add(14, y, w);
                    };
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), FLAGS);
                }
            }
            BlockPos lamp = switch (dir) {
                case NORTH -> base.add(12, 4, 5);
                case SOUTH -> base.add(12, 4, 19);
                case WEST -> base.add(5, 4, 12);
                default -> base.add(19, 4, 12);
            };
            world.setBlockState(lamp, LANTERN, FLAGS);
        }
        world.setBlockState(base.add(12, 4, 12), LANTERN, FLAGS);
    }

    /** Hollow tube: floor y=0, walls on the border y=1..4, ceiling y=5, ends left open. */
    private static void box(ServerWorld world, BlockPos base, Random random, int x1, int z1, int x2, int z2) {
        boolean alongZ = (z2 - z1) > (x2 - x1);
        boolean alongX = (x2 - x1) > (z2 - z1);
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                world.setBlockState(base.add(x, 0, z), random.nextInt(8) == 0 ? FLOOR_CRACKED : FLOOR, FLAGS);
                world.setBlockState(base.add(x, 5, z), CEILING, FLAGS);
                boolean border;
                if (alongZ) border = x == x1 || x == x2;
                else if (alongX) border = z == z1 || z == z2;
                else border = x == x1 || x == x2 || z == z1 || z == z2;
                for (int y = 1; y <= 4; y++) {
                    BlockPos p = base.add(x, y, z);
                    if (border) {
                        world.setBlockState(p, random.nextInt(7) == 0 ? WALL_CRACKED : WALL, FLAGS);
                    } else if (!world.getBlockState(p).isOf(Blocks.SOUL_LANTERN)) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), FLAGS);
                    }
                }
            }
        }
    }
}

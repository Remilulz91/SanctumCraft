package fr.sanctumcraft.portal;

import fr.sanctumcraft.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Rectangular rune stone frame, Nether-portal style. Corners are not required.
 * Interior: 2..21 wide, 3..21 tall, must be air.
 */
public record PortalShape(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {

    public static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;

    /** Tries to find a valid frame whose interior contains {@code start}. */
    public static Optional<PortalShape> find(World world, BlockPos start) {
        Optional<PortalShape> x = tryAxis(world, start, Direction.Axis.X);
        return x.isPresent() ? x : tryAxis(world, start, Direction.Axis.Z);
    }

    private static Optional<PortalShape> tryAxis(World world, BlockPos start, Direction.Axis axis) {
        if (!isEmpty(world.getBlockState(start))) return Optional.empty();
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();

        // 1. Go down to the bottom of the interior.
        BlockPos p = start;
        int guard = 0;
        while (p.getY() > world.getBottomY() && isEmpty(world.getBlockState(p.down())) && guard++ < MAX_HEIGHT) {
            p = p.down();
        }
        if (!isFrame(world.getBlockState(p.down()))) return Optional.empty();

        // 2. Go left along the bottom row.
        guard = 0;
        while (isEmpty(world.getBlockState(p.offset(left))) && isFrame(world.getBlockState(p.offset(left).down()))
                && guard++ < MAX_WIDTH) {
            p = p.offset(left);
        }
        if (!isFrame(world.getBlockState(p.offset(left)))) return Optional.empty();
        BlockPos bottomLeft = p;

        // 3. Measure width.
        int width = 0;
        while (width <= MAX_WIDTH
                && isEmpty(world.getBlockState(bottomLeft.offset(right, width)))
                && isFrame(world.getBlockState(bottomLeft.offset(right, width).down()))) {
            width++;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) return Optional.empty();
        if (!isFrame(world.getBlockState(bottomLeft.offset(right, width)))) return Optional.empty();

        // 4. Measure height: rows whose interior is empty and whose sides are frame.
        int height = 0;
        while (height <= MAX_HEIGHT) {
            boolean rowEmpty = true;
            for (int x = 0; x < width; x++) {
                if (!isEmpty(world.getBlockState(bottomLeft.offset(right, x).up(height)))) {
                    rowEmpty = false;
                    break;
                }
            }
            if (!rowEmpty) break;
            if (!isFrame(world.getBlockState(bottomLeft.offset(left).up(height)))
                    || !isFrame(world.getBlockState(bottomLeft.offset(right, width).up(height)))) {
                return Optional.empty();
            }
            height++;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) return Optional.empty();

        // 5. Top row must be frame.
        for (int x = 0; x < width; x++) {
            if (!isFrame(world.getBlockState(bottomLeft.offset(right, x).up(height)))) return Optional.empty();
        }
        return Optional.of(new PortalShape(bottomLeft, axis, width, height));
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir();
    }

    private static boolean isFrame(BlockState state) {
        return state.isOf(ModBlocks.RUNE_STONE);
    }

    public Direction right() {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /** Fills the interior with portal blocks (no neighbour updates, so it does not break itself). */
    public void fill(World world) {
        BlockState portal = ModBlocks.SANCTUM_PORTAL.getDefaultState().with(SanctumPortalBlock.AXIS, axis);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                world.setBlockState(bottomLeft.offset(right(), x).up(y), portal,
                        Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
    }

    /** Bounding box of the portal interior. */
    public BlockBox box() {
        BlockPos max = bottomLeft.offset(right(), width - 1).up(height - 1);
        return BlockBox.create(bottomLeft, max);
    }
}

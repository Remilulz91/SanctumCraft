package fr.sanctumcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Nest of a sanctum. ACTIVE = an egg is buried inside (hit it to dig it out, see NestHandler).
 * Unbreakable.
 */
public class EggNestBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private static final VoxelShape BASE = Block.createCuboidShape(0, 0, 0, 16, 6, 16);
    private static final VoxelShape WITH_EGG = VoxelShapes.union(BASE, Block.createCuboidShape(5, 6, 5, 11, 14, 11));

    public EggNestBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(ACTIVE) ? WITH_EGG : BASE;
    }
}

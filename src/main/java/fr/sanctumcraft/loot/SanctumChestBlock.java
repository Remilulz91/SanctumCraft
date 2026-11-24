package fr.sanctumcraft.loot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Sanctum chest of a given rarity. Loot is rolled on first opening and revealed item by item. */
public class SanctumChestBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPENED = BooleanProperty.of("opened");
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);

    public final ChestTier tier;

    public SanctumChestBlock(ChestTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPENED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPENED);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public MutableText getName() {
        return Text.translatable(getTranslationKey()).formatted(tier.color);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            LootHandler.openChest(sp, pos, this);
        }
        return ActionResult.SUCCESS;
    }
}

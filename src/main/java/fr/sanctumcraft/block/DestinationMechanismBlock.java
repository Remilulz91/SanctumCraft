package fr.sanctumcraft.block;

import fr.sanctumcraft.instance.InstanceManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Right-click (and stay close) to open the second exit portal of the sanctum. Unbreakable. */
public class DestinationMechanismBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public DestinationMechanismBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (state.get(ACTIVE)) return ActionResult.PASS;
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            InstanceManager.startDestinationActivation(sp, pos);
        }
        return ActionResult.SUCCESS;
    }
}

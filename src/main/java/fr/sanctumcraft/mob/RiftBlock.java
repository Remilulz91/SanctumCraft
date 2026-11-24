package fr.sanctumcraft.mob;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** A rift: right-click to trigger waves of monsters; survive them to get a chest. */
public class RiftBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public RiftBlock(Settings settings) {
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
            RiftManager.start(sp, pos);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int n = state.get(ACTIVE) ? 6 : 2;
        for (int i = 0; i < n; i++) {
            world.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0, 0.05 + random.nextDouble() * 0.1, 0);
        }
    }
}

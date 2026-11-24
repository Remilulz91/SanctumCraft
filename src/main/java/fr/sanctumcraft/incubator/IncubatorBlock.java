package fr.sanctumcraft.incubator;

import fr.sanctumcraft.companion.Companions;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggRarity;
import fr.sanctumcraft.egg.SanctumEggItem;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Incubator: put a secured Sanctum egg in it, wait (game time: it keeps going while players are
 * offline, not while the server is off), then right-click to hatch a companion.
 * STAGE: 0 = empty, 1 = incubating, 2 = ready.
 */
public class IncubatorBlock extends Block implements BlockEntityProvider {

    public static final IntProperty STAGE = IntProperty.of("stage", 0, 2);

    public IncubatorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(STAGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IncubatorBlockEntity(pos, state);
    }

    public static long durationTicks(EggRarity r) {
        int minutes = switch (r) {
            case COMMON -> 10;
            case RARE -> 20;
            case EPIC -> 40;
            case LEGENDARY -> 60;
        };
        return Math.max(20, Math.round(minutes * 1200L * SanctumConfig.incubationTimePercent / 100.0));
    }

    private static long now(World world) {
        return world.getServer() != null ? world.getServer().getOverworld().getTime() : world.getTime();
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof SanctumEggItem egg)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.get(STAGE) != 0) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (!SanctumEggItem.isSecured(stack)) {
            player.sendMessage(Text.translatable("message.sanctumcraft.incubator.unsecured").formatted(Formatting.RED), true);
            return ItemActionResult.FAIL;
        }
        if (world.getBlockEntity(pos) instanceof IncubatorBlockEntity be) {
            be.rarity = egg.rarity.name();
            be.start = now(world);
            be.duration = durationTicks(egg.rarity);
            be.markDirty();
            world.setBlockState(pos, state.with(STAGE, 1), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, (int) Math.min(Integer.MAX_VALUE, be.duration));
            if (!player.getAbilities().creativeMode) stack.decrement(1);
            world.playSound(null, pos, SoundEvents.BLOCK_SNIFFER_EGG_PLOP, SoundCategory.BLOCKS, 1.0F, 1.0F);
            player.sendMessage(Text.translatable("message.sanctumcraft.incubator.started",
                    InstanceManager.formatTime(be.duration)).formatted(Formatting.GREEN), true);
        }
        return ItemActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof IncubatorBlockEntity be)) return ActionResult.PASS;
        int stage = state.get(STAGE);
        if (stage == 0) {
            player.sendMessage(Text.translatable("message.sanctumcraft.incubator.empty").formatted(Formatting.GRAY), true);
        } else if (stage == 1) {
            long left = be.start + be.duration - now(world);
            if (left <= 0) {
                world.setBlockState(pos, state.with(STAGE, 2), Block.NOTIFY_ALL);
            } else {
                player.sendMessage(Text.translatable("message.sanctumcraft.incubator.waiting",
                        InstanceManager.formatTime(left)).formatted(Formatting.YELLOW), true);
            }
        } else if (player instanceof ServerPlayerEntity sp) {
            EggRarity rarity = EggRarity.valueOf(be.rarity);
            be.rarity = "";
            be.markDirty();
            world.setBlockState(pos, state.with(STAGE, 0), Block.NOTIFY_ALL);
            Companions.hatch(sp, (ServerWorld) world, pos.up(), rarity);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(STAGE) != 1 || !(world.getBlockEntity(pos) instanceof IncubatorBlockEntity be)) return;
        long left = be.start + be.duration - now(world);
        if (left > 0) {
            world.scheduleBlockTick(pos, this, (int) Math.min(Integer.MAX_VALUE, left));
            return;
        }
        world.setBlockState(pos, state.with(STAGE, 2), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_SNIFFER_EGG_CRACK, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof IncubatorBlockEntity be
                && !be.rarity.isEmpty()) {
            // Breaking the incubator gives the (secured) egg back.
            ItemStack egg = new ItemStack(ModItems.eggFor(EggRarity.valueOf(be.rarity)));
            SanctumEggItem.secure(egg);
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, egg);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int stage = state.get(STAGE);
        if (stage == 0) return;
        world.addParticle(stage == 2 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.WHITE_ASH,
                pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                0, 0.02, 0);
    }
}

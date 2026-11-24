package fr.sanctumcraft.instance;

import fr.sanctumcraft.registry.ModDimensions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * Inside a sanctum (non-creative players):
 * - the sanctum itself can't be broken; blocks placed by players can be placed AND broken back;
 * - no buckets (fluids would wash the rooms) and no pistons (they would move the walls);
 * - no hoe / shovel / axe conversions on sanctum blocks.
 * Explosions are filtered by {@link fr.sanctumcraft.mixin.ExplosionMixin}.
 */
public final class SanctumProtection {

    private SanctumProtection() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!ModDimensions.isSanctum(world) || player.isCreative()) return true;
            if (world instanceof ServerWorld sw && InstanceManager.isPlayerPlaced(sw, pos)) return true;
            player.sendMessage(Text.translatable("message.sanctumcraft.unbreakable").formatted(Formatting.GRAY), true);
            return false;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld sw && ModDimensions.isSanctum(world)) {
                InstanceManager.unmarkPlaced(sw, pos);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!ModDimensions.isSanctum(world) || player.isCreative()) return ActionResult.PASS;
            ItemStack stack = player.getStackInHand(hand);

            if (stack.getItem() instanceof HoeItem || stack.getItem() instanceof ShovelItem
                    || stack.getItem() instanceof AxeItem) {
                if (!(world instanceof ServerWorld sw) || !InstanceManager.isPlayerPlaced(sw, hit.getBlockPos())) {
                    return ActionResult.FAIL;
                }
            }

            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == Blocks.PISTON || blockItem.getBlock() == Blocks.STICKY_PISTON) {
                    player.sendMessage(Text.translatable("message.sanctumcraft.forbidden_item").formatted(Formatting.GRAY), true);
                    return ActionResult.FAIL;
                }
                // Remember where the block will land so the player can break it back later.
                // (If the click opens a container instead, the position stays air and nothing happens.)
                if (world instanceof ServerWorld sw) {
                    BlockPos target = new ItemPlacementContext(player, hand, stack, hit).getBlockPos();
                    InstanceManager.markPlaced(sw, target);
                }
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (ModDimensions.isSanctum(world) && !player.isCreative() && stack.getItem() instanceof BucketItem) {
                player.sendMessage(Text.translatable("message.sanctumcraft.forbidden_item").formatted(Formatting.GRAY), true);
                return TypedActionResult.fail(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }
}

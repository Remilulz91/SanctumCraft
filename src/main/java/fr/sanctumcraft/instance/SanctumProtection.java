package fr.sanctumcraft.instance;

import fr.sanctumcraft.registry.ModDimensions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
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
 * - no TNT, TNT minecarts, beds or respawn anchors (explosives), no creepers at all;
 * - no hoe / shovel / axe conversions on sanctum blocks.
 * Explosions are filtered by {@link fr.sanctumcraft.mixin.ExplosionMixin}.
 */
public final class SanctumProtection {

    private SanctumProtection() {}

    private static boolean isForbiddenBlock(Block block) {
        return block == Blocks.PISTON || block == Blocks.STICKY_PISTON
                || block == Blocks.TNT || block == Blocks.RESPAWN_ANCHOR || block == Blocks.ENDER_CHEST
                || block instanceof BedBlock;
    }

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

            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.ENDER_CHEST)) {
                // An ender chest would let players stash eggs and skip the extraction.
                player.sendMessage(Text.translatable("message.sanctumcraft.forbidden_item").formatted(Formatting.GRAY), true);
                return ActionResult.FAIL;
            }

            if (stack.isOf(Items.TNT_MINECART)
                    || (stack.getItem() instanceof SpawnEggItem egg && egg.getEntityType(stack) == EntityType.CREEPER)) {
                player.sendMessage(Text.translatable("message.sanctumcraft.forbidden_item").formatted(Formatting.GRAY), true);
                return ActionResult.FAIL;
            }

            if (stack.getItem() instanceof BlockItem blockItem) {
                if (isForbiddenBlock(blockItem.getBlock())) {
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

        // Safety net: no creeper may exist in the sanctum (spawners, commands, portals...).
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof CreeperEntity && ModDimensions.isSanctum(world)) {
                entity.discard();
            }
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

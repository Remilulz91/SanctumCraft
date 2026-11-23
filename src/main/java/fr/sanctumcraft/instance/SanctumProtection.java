package fr.sanctumcraft.instance;

import fr.sanctumcraft.registry.ModDimensions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

/**
 * The sanctum is not a building area: survival players can't break or place blocks there
 * (creative players can, to edit rooms).
 */
public final class SanctumProtection {

    private SanctumProtection() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !ModDimensions.isSanctum(world) || player.isCreative());

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!ModDimensions.isSanctum(world) || player.isCreative()) return ActionResult.PASS;
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof BlockItem || stack.getItem() instanceof FlintAndSteelItem) {
                // Sneak + block item would place a block; plain click still opens containers.
                if (player.isSneaking() || world.getBlockState(hit.getBlockPos()).createScreenHandlerFactory(world, hit.getBlockPos()) == null) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (ModDimensions.isSanctum(world) && !player.isCreative() && stack.getItem() instanceof BucketItem) {
                return TypedActionResult.fail(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }
}

package fr.sanctumcraft.loot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Locked door of a vault room. Use the matching key on it: the whole doorway opens. */
public class VaultDoorBlock extends Block {

    public final KeyTier tier;

    public VaultDoorBlock(KeyTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SanctumKeyItem key && key.tier == tier) {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                LootHandler.openVault(sp, pos, this, stack);
            }
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.translatable("message.sanctumcraft.vault.locked",
                    Text.translatable("item.sanctumcraft." + tier.name().toLowerCase() + "_key").formatted(tier.color))
                    .formatted(Formatting.GRAY), true);
        }
        return ActionResult.SUCCESS;
    }
}

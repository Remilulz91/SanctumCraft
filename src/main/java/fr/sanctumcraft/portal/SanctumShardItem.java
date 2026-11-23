package fr.sanctumcraft.portal;

import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.registry.ModDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

/** Right-click a rune stone frame with it to light a sanctum portal. Consumed on success. */
public class SanctumShardItem extends Item {

    public SanctumShardItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clicked = context.getBlockPos();
        if (!world.getBlockState(clicked).isOf(ModBlocks.RUNE_STONE)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        PlayerEntity player = context.getPlayer();
        if (ModDimensions.isSanctum(world)) {
            if (player != null) {
                player.sendMessage(Text.translatable("message.sanctumcraft.portal.no_nesting").formatted(Formatting.RED), true);
            }
            return ActionResult.FAIL;
        }

        BlockPos start = clicked.offset(context.getSide());
        Optional<PortalShape> shape = PortalShape.find(world, start);
        if (shape.isEmpty()) {
            if (player != null) {
                player.sendMessage(Text.translatable("message.sanctumcraft.portal.invalid_frame").formatted(Formatting.RED), true);
            }
            return ActionResult.FAIL;
        }

        shape.get().fill(world);
        InstanceManager.registerPortal((ServerWorld) world, shape.get());
        world.playSound(null, start, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.8F, 1.2F);
        if (player != null && !player.getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        if (player != null) {
            player.sendMessage(Text.translatable("message.sanctumcraft.portal.lit").formatted(Formatting.LIGHT_PURPLE), true);
        }
        return ActionResult.CONSUME;
    }
}

package fr.sanctumcraft.guide;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Right-click: opens the guide (always up to date with the installed mod version). */
public class SanctumGuideItem extends Item {

    public SanctumGuideItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (SanctumGuide.refresh(stack)) {
                // Send the updated book to the client before asking it to open it.
                player.currentScreenHandler.sendContentUpdates();
            }
            player.networkHandler.sendPacket(new OpenWrittenBookS2CPacket(hand));
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}

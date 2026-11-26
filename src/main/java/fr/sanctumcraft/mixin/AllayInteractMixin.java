package fr.sanctumcraft.mixin;

import fr.sanctumcraft.companion.Companions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An allay companion never takes the item you hold out to it. */
@Mixin(AllayEntity.class)
public abstract class AllayInteractMixin {

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$noGift(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (((Entity) (Object) this).getCommandTags().contains(Companions.TAG)) cir.setReturnValue(ActionResult.PASS);
    }
}

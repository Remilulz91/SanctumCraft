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
        if (Companions.isCompanion((Entity) (Object) this)) cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Inject(method = "loot", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$noLoot(net.minecraft.entity.ItemEntity item, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (Companions.isCompanion((Entity) (Object) this)) ci.cancel();
    }
}

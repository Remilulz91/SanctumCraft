package fr.sanctumcraft.mixin;

import fr.sanctumcraft.instance.KeepInventoryHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** sanctumKeepInventory: no item / XP drop for deaths inside a sanctum. */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$keepInventory(CallbackInfo ci) {
        if (KeepInventoryHandler.shouldKeep((PlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "getXpToDrop", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$keepXp(CallbackInfoReturnable<Integer> cir) {
        if (KeepInventoryHandler.shouldKeep((PlayerEntity) (Object) this)) {
            cir.setReturnValue(0);
        }
    }
}

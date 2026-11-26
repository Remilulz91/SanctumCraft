package fr.sanctumcraft.mixin;

import fr.sanctumcraft.companion.Companions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An allay companion never picks up or collects items. */
@Mixin(AllayEntity.class)
public abstract class AllayEntityMixin {

    @Inject(method = "canPickUpLoot", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$noLoot(CallbackInfoReturnable<Boolean> cir) {
        if (Companions.isCompanion((Entity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "canGather", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$noGather(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Companions.isCompanion((Entity) (Object) this)) cir.setReturnValue(false);
    }
}

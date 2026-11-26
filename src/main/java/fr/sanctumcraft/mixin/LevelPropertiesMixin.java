package fr.sanctumcraft.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The sanctum dimension is declared by data pack, which Minecraft flags as "experimental".
 * Report the world as stable so loading it doesn't show the experimental-settings warning.
 */
@Mixin(LevelProperties.class)
public abstract class LevelPropertiesMixin {

    @Inject(method = "getLifecycle", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$stable(CallbackInfoReturnable<Lifecycle> cir) {
        cir.setReturnValue(Lifecycle.stable());
    }
}

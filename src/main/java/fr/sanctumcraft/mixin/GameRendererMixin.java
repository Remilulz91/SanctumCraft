package fr.sanctumcraft.mixin;

import fr.sanctumcraft.rolling.RollingEgg;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No hand / held item in first person while rolling. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$hideHand(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        if (RollingEgg.clientLocalRolling) ci.cancel();
    }
}

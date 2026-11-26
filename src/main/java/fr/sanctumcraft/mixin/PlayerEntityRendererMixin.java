package fr.sanctumcraft.mixin;

import fr.sanctumcraft.client.RollingEggRenderer;
import fr.sanctumcraft.rolling.RollingEgg;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A rolling player is drawn as a big rolling egg (no body, armor, held items or name). */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$rollingEgg(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                                         VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (RollingEgg.CLIENT_ROLLING.contains(player.getUuid())) {
            RollingEggRenderer.render(player, tickDelta, matrices, consumers, light);
            ci.cancel();
        }
    }
}

package fr.sanctumcraft.client;

import fr.sanctumcraft.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Draws a rolling player as a large egg spinning in its direction of travel. */
public final class RollingEggRenderer {

    private static ItemStack egg;

    private RollingEggRenderer() {}

    public static void render(AbstractClientPlayerEntity player, float tickDelta, MatrixStack matrices,
                              VertexConsumerProvider consumers, int light) {
        if (egg == null) egg = new ItemStack(ModItems.EGG_EPIC);
        float bodyYaw = player.getYaw(tickDelta);
        // horizontalSpeed grows with the distance walked: turn it into a roll angle (radius ~ 0.45 block).
        float distance = MathHelper.lerp(tickDelta, player.prevHorizontalSpeed, player.horizontalSpeed) / 0.6F;
        float roll = distance / 0.45F * MathHelper.DEGREES_PER_RADIAN;

        matrices.push();
        matrices.translate(0.0, 0.5, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-roll));
        matrices.scale(1.8F, 1.8F, 1.8F);
        MinecraftClient.getInstance().getItemRenderer().renderItem(egg, ModelTransformationMode.FIXED, light,
                OverlayTexture.DEFAULT_UV, matrices, consumers, player.getWorld(), player.getId());
        matrices.pop();
    }
}

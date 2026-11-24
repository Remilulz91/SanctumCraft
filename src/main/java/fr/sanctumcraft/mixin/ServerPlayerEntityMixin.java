package fr.sanctumcraft.mixin;

import fr.sanctumcraft.instance.InstanceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Refuses any game mode change (command, F3+F4...) other than survival inside a sanctum. */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "changeGameMode", at = @At("HEAD"), cancellable = true)
    private void sanctumcraft$lockGameMode(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (InstanceManager.blocksGameModeChange(self, gameMode)) {
            self.sendMessage(Text.translatable("message.sanctumcraft.gamemode_locked").formatted(Formatting.RED), true);
            cir.setReturnValue(false);
        }
    }
}

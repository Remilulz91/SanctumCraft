package fr.sanctumcraft.mixin;

import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.registry.ModDimensions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Explosions in a sanctum only destroy blocks placed by players, never the rooms. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final private World world;

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void sanctumcraft$protectRooms(boolean particles, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && ModDimensions.isSanctum(serverWorld)) {
            ((Explosion) (Object) this).getAffectedBlocks()
                    .removeIf(pos -> !InstanceManager.isPlayerPlaced(serverWorld, pos));
        }
    }
}

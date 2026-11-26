package fr.sanctumcraft.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Same as LevelPropertiesMixin, for the warning shown when creating a new world. */
@Mixin(IntegratedServerLoader.class)
public abstract class IntegratedServerLoaderMixin {

    @ModifyVariable(method = "tryLoad", at = @At("HEAD"), argsOnly = true)
    private static Lifecycle sanctumcraft$stable(Lifecycle lifecycle) {
        return Lifecycle.stable();
    }
}

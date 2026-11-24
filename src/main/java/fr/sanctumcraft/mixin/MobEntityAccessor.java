package fr.sanctumcraft.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to the AI goal lists, to turn vanilla mobs into companions. */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {

    @Accessor("goalSelector")
    GoalSelector sanctumcraft$getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector sanctumcraft$getTargetSelector();
}

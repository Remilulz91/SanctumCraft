package fr.sanctumcraft.companion;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumSet;
import java.util.function.Supplier;

/** Walks back to the owner when too far (the per-second manager teleports it when much too far). */
public class CompanionFollowGoal extends Goal {

    private final MobEntity mob;
    private final Supplier<ServerPlayerEntity> owner;
    private int recalc;

    public CompanionFollowGoal(MobEntity mob, Supplier<ServerPlayerEntity> owner) {
        this.mob = mob;
        this.owner = owner;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        ServerPlayerEntity o = owner.get();
        return o != null && o.getWorld() == mob.getWorld() && mob.getTarget() == null && mob.squaredDistanceTo(o) > 49;
    }

    @Override
    public boolean shouldContinue() {
        ServerPlayerEntity o = owner.get();
        return o != null && o.getWorld() == mob.getWorld() && mob.getTarget() == null && mob.squaredDistanceTo(o) > 9;
    }

    @Override
    public void tick() {
        ServerPlayerEntity o = owner.get();
        if (o == null) return;
        mob.getLookControl().lookAt(o, 10.0F, mob.getMaxLookPitchChange());
        if (--recalc <= 0) {
            recalc = 10;
            mob.getNavigation().startMovingTo(o, 1.2);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}

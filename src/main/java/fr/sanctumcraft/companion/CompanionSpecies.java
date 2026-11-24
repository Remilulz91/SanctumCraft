package fr.sanctumcraft.companion;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

/** Vanilla mobs used as companions, with their role and possible traits. */
public enum CompanionSpecies {
    WOLF(EntityType.WOLF, Items.WOLF_SPAWN_EGG,
            List.of(CompanionTrait.FEROCIOUS, CompanionTrait.STURDY, CompanionTrait.SWIFT, CompanionTrait.GUARDIAN, CompanionTrait.HEALER)),
    FOX(EntityType.FOX, Items.FOX_SPAWN_EGG,
            List.of(CompanionTrait.SWIFT, CompanionTrait.TRACKER, CompanionTrait.FEROCIOUS, CompanionTrait.GUARDIAN)),
    ALLAY(EntityType.ALLAY, Items.ALLAY_SPAWN_EGG,
            List.of(CompanionTrait.HEALER, CompanionTrait.TRACKER, CompanionTrait.GUARDIAN, CompanionTrait.SWIFT)),
    IRON_GOLEM(EntityType.IRON_GOLEM, Items.IRON_GOLEM_SPAWN_EGG,
            List.of(CompanionTrait.STURDY, CompanionTrait.FEROCIOUS, CompanionTrait.GUARDIAN));

    public final EntityType<? extends MobEntity> type;
    public final Item icon;
    public final List<CompanionTrait> traits;

    CompanionSpecies(EntityType<? extends MobEntity> type, Item icon, List<CompanionTrait> traits) {
        this.type = type;
        this.icon = icon;
        this.traits = traits;
    }

    public String key() {
        return "species.sanctumcraft." + name().toLowerCase();
    }
}

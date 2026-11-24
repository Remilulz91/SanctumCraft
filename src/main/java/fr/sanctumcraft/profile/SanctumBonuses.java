package fr.sanctumcraft.profile;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.companion.Companions;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.relic.Relic;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every second: talent and showcase bonuses for players inside an active sanctum (removed outside),
 * plus the dynamic companion traits.
 */
public final class SanctumBonuses {

    private static final Identifier HEALTH_ID = SanctumCraft.id("talent_health");
    private static final Identifier ARMOR_ID = SanctumCraft.id("talent_armor");
    private static final Identifier SPEED_ID = SanctumCraft.id("talent_speed");
    private static final Map<UUID, Integer> SECOND_WIND_READY = new HashMap<>();

    private SanctumBonuses() {}

    public static void tick(MinecraftServer server) {
        int ticks = server.getTicks();
        if (ticks % 20 != 0) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            SanctumInstance inst = InstanceManager.instanceOf(p);
            if (inst == null || inst.status != SanctumInstance.Status.ACTIVE || !p.isAlive()) {
                set(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, 0, EntityAttributeModifier.Operation.ADD_VALUE);
                set(p, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, 0, EntityAttributeModifier.Operation.ADD_VALUE);
                set(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, 0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                continue;
            }
            HunterProfile prof = Profiles.of(p);
            double health = 4.0 * prof.rank(Talent.VITALITY) + (Relic.rowComplete(prof, 0) ? 4.0 : 0.0);
            double armor = 2.0 * prof.rank(Talent.TOUGHNESS) + (Relic.rowComplete(prof, 1) ? 4.0 : 0.0);
            double speed = 0.05 * prof.rank(Talent.FLEET);
            set(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, health, EntityAttributeModifier.Operation.ADD_VALUE);
            set(p, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, armor, EntityAttributeModifier.Operation.ADD_VALUE);
            set(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, speed, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            int wind = prof.rank(Talent.SECOND_WIND);
            if (wind > 0 && p.getHealth() < p.getMaxHealth() * 0.3F
                    && SECOND_WIND_READY.getOrDefault(p.getUuid(), 0) <= ticks) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, wind - 1));
                SECOND_WIND_READY.put(p.getUuid(), ticks + 20 * 60);
            }
            int eyes = prof.rank(Talent.NIGHT_EYES);
            if (eyes > 0) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false, true));
                if (eyes >= 2 && ticks % 600 == 0) {
                    Companions.pointToNest(p, inst, Text.translatable("message.sanctumcraft.talent.night_eyes"));
                }
            }
            Companions.tickInSanctum(p, inst, ticks);
        }
    }

    private static void set(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id, double value,
                            EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst == null) return;
        EntityAttributeModifier current = inst.getModifier(id);
        if (value == 0) {
            if (current != null) inst.removeModifier(id);
            return;
        }
        if (current != null && current.value() == value) return;
        inst.removeModifier(id);
        inst.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}

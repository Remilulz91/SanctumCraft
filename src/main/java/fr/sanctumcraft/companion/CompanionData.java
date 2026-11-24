package fr.sanctumcraft.companion;

import fr.sanctumcraft.egg.EggRarity;
import net.minecraft.nbt.NbtCompound;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** A companion of a player's collection (no entity until summoned). */
public final class CompanionData {

    public final UUID id;
    public final CompanionSpecies species;
    public final EggRarity rarity;
    /** Stat multipliers rolled at hatching (1.0 = normal). */
    public final double health, damage, speed;
    /** Trait -> power (0.5 .. 1.5). */
    public final Map<CompanionTrait, Double> traits;
    /** Overworld game time until which the companion is knocked out. */
    public long koUntil;

    public CompanionData(UUID id, CompanionSpecies species, EggRarity rarity, double health, double damage, double speed,
                         Map<CompanionTrait, Double> traits) {
        this.id = id;
        this.species = species;
        this.rarity = rarity;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.traits = traits;
    }

    public double power(CompanionTrait t) {
        return traits.getOrDefault(t, 0.0);
    }

    public NbtCompound toNbt() {
        NbtCompound t = new NbtCompound();
        t.putUuid("id", id);
        t.putString("species", species.name());
        t.putString("rarity", rarity.name());
        t.putDouble("health", health);
        t.putDouble("damage", damage);
        t.putDouble("speed", speed);
        NbtCompound tr = new NbtCompound();
        traits.forEach((k, v) -> tr.putDouble(k.name(), v));
        t.put("traits", tr);
        t.putLong("koUntil", koUntil);
        return t;
    }

    public static CompanionData fromNbt(NbtCompound t) {
        Map<CompanionTrait, Double> traits = new EnumMap<>(CompanionTrait.class);
        NbtCompound tr = t.getCompound("traits");
        for (String k : tr.getKeys()) {
            try {
                traits.put(CompanionTrait.valueOf(k), tr.getDouble(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        CompanionSpecies species;
        EggRarity rarity;
        try {
            species = CompanionSpecies.valueOf(t.getString("species"));
            rarity = EggRarity.valueOf(t.getString("rarity"));
        } catch (IllegalArgumentException e) {
            species = CompanionSpecies.WOLF;
            rarity = EggRarity.COMMON;
        }
        CompanionData d = new CompanionData(t.getUuid("id"), species, rarity, t.getDouble("health"),
                t.getDouble("damage"), t.getDouble("speed"), traits);
        d.koUntil = t.getLong("koUntil");
        return d;
    }
}

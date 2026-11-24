package fr.sanctumcraft.instance;

import fr.sanctumcraft.config.SanctumConfig;
import net.minecraft.util.Formatting;

/**
 * Difficulty chosen when opening a sanctum.
 * Today it changes the timer; the multipliers are applied to mobs and loot from 0.3/0.4 on.
 */
public enum SanctumDifficulty {
    NORMAL(Formatting.GREEN, 1.0F, 1.0F, 1.0F, 1.0F),
    NIGHTMARE(Formatting.DARK_PURPLE, 1.75F, 1.5F, 1.5F, 1.5F),
    CHAOS(Formatting.DARK_RED, 2.75F, 2.25F, 2.0F, 2.0F);

    public final Formatting color;
    /** Mob max health multiplier (0.4). */
    public final float mobHealth;
    /** Mob damage multiplier (0.4). */
    public final float mobDamage;
    /** Number of mobs multiplier (0.4). */
    public final float mobCount;
    /** Loot quality multiplier (0.3). */
    public final float loot;

    SanctumDifficulty(Formatting color, float mobHealth, float mobDamage, float mobCount, float loot) {
        this.color = color;
        this.mobHealth = mobHealth;
        this.mobDamage = mobDamage;
        this.mobCount = mobCount;
        this.loot = loot;
    }

    public int durationSeconds() {
        return switch (this) {
            case NORMAL -> SanctumConfig.raidDurationSeconds;
            case NIGHTMARE -> SanctumConfig.raidDurationNightmareSeconds;
            case CHAOS -> SanctumConfig.raidDurationChaosSeconds;
        };
    }

    public String key() {
        return "difficulty.sanctumcraft." + name().toLowerCase();
    }

    public static SanctumDifficulty byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return NORMAL;
        }
    }
}

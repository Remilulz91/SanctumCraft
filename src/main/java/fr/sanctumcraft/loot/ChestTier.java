package fr.sanctumcraft.loot;

import fr.sanctumcraft.instance.SanctumDifficulty;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

/** Rarity of a sanctum chest: decides its color, its loot table and its chance to hold a key. */
public enum ChestTier {
    BASIC(Formatting.GRAY, 2),
    FINE(Formatting.GREEN, 5),
    RARE(Formatting.BLUE, 10),
    EPIC(Formatting.DARK_PURPLE, 20),
    LEGENDARY(Formatting.GOLD, 35);

    public final Formatting color;
    /** Percent chance that the chest also holds a key of the run's tier. */
    public final int keyChance;

    ChestTier(Formatting color, int keyChance) {
        this.color = color;
        this.keyChance = keyChance;
    }

    public String id() {
        return name().toLowerCase();
    }

    public ChestTier bump(int steps) {
        return values()[Math.min(values().length - 1, ordinal() + steps)];
    }

    public static ChestTier roll(Random random, SanctumDifficulty difficulty) {
        int[] w = switch (difficulty) {
            case NORMAL -> new int[]{45, 30, 17, 7, 1};
            case NIGHTMARE -> new int[]{25, 30, 25, 15, 5};
            case CHAOS -> new int[]{10, 20, 30, 27, 13};
        };
        int total = 0;
        for (int v : w) total += v;
        int r = random.nextInt(total);
        for (int i = 0; i < w.length; i++) {
            if (r < w[i]) return values()[i];
            r -= w[i];
        }
        return BASIC;
    }
}

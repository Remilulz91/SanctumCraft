package fr.sanctumcraft.egg;

import fr.sanctumcraft.instance.SanctumDifficulty;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

public enum EggRarity {
    COMMON(Formatting.WHITE, 40, 0),
    RARE(Formatting.AQUA, 100, 0),
    EPIC(Formatting.LIGHT_PURPLE, 250, 1),
    LEGENDARY(Formatting.GOLD, 600, 1);

    public final Formatting color;
    /** XP points granted when the egg is extracted. */
    public final int extractionXp;
    /** Extra slowness level while carried in the sanctum. */
    public final int weight;

    EggRarity(Formatting color, int extractionXp, int weight) {
        this.color = color;
        this.extractionXp = extractionXp;
        this.weight = weight;
    }

    public String key() {
        return "rarity.sanctumcraft." + name().toLowerCase();
    }

    /** Weights (common, rare, epic, legendary) per difficulty. */
    public static EggRarity roll(Random random, SanctumDifficulty difficulty) {
        int[] w = switch (difficulty) {
            case NORMAL -> new int[]{60, 30, 9, 1};
            case NIGHTMARE -> new int[]{35, 37, 22, 6};
            case CHAOS -> new int[]{15, 35, 33, 17};
        };
        int total = w[0] + w[1] + w[2] + w[3];
        int r = random.nextInt(total);
        for (int i = 0; i < 4; i++) {
            if (r < w[i]) return values()[i];
            r -= w[i];
        }
        return COMMON;
    }
}

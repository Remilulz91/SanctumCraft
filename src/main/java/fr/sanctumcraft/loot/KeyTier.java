package fr.sanctumcraft.loot;

import fr.sanctumcraft.instance.SanctumDifficulty;
import net.minecraft.util.Formatting;

/** Keys and vault doors. Each difficulty uses its own key tier. */
public enum KeyTier {
    RUSTED(Formatting.GOLD),
    SILVER(Formatting.WHITE),
    GILDED(Formatting.YELLOW);

    public final Formatting color;

    KeyTier(Formatting color) {
        this.color = color;
    }

    public static KeyTier forDifficulty(SanctumDifficulty d) {
        return switch (d) {
            case NORMAL -> RUSTED;
            case NIGHTMARE -> SILVER;
            case CHAOS -> GILDED;
        };
    }
}

package fr.sanctumcraft.relic;

import fr.sanctumcraft.profile.HunterProfile;

/**
 * Collectible relics, found in epic and legendary sanctum chests. Each exists as a normal
 * version and (rarely) a collector version; only collector relics go in the showcase.
 * Showcase rows: 0 = Ruins, 1 = Depths, 2 = Nest.
 */
public enum Relic {
    STONE_MASK(0), AMETHYST_IDOL(0), RUNIC_TABLET(0), TARNISHED_CHALICE(0),
    ANCIENT_LANTERN(1), CRYSTAL_SKULL(1), BROKEN_COMPASS(1), CRACKED_SCEPTER(1),
    DAWN_FEATHER(2), FOSSIL_SHELL(2), GOLDEN_NEST(2), SANCTUM_CROWN(2);

    public final int row;

    Relic(int row) {
        this.row = row;
    }

    public String id() {
        return "relic_" + name().toLowerCase();
    }

    public static boolean rowComplete(HunterProfile p, int row) {
        for (Relic r : values()) {
            if (r.row == row && !p.showcase.contains(r.name())) return false;
        }
        return true;
    }
}

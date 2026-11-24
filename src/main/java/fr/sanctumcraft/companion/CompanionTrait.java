package fr.sanctumcraft.companion;

/**
 * Companion traits, each with a random power (0.5 .. 1.5) rolled at hatching.
 * FEROCIOUS / STURDY / SWIFT change the stats; HEALER / TRACKER / GUARDIAN act in the sanctum.
 */
public enum CompanionTrait {
    /** +25 % attack damage (x power). */
    FEROCIOUS,
    /** +50 % health (x power). */
    STURDY,
    /** +20 % speed (x power). */
    SWIFT,
    /** Heals its owner (1 heart x power every 10 s) in the sanctum. */
    HEALER,
    /** Points to the nearest active nest every 20 s in the sanctum. */
    TRACKER,
    /** Owner gets Resistance I while the companion is close, in the sanctum. */
    GUARDIAN;

    public String tag() {
        return "sc_trait_" + name().toLowerCase();
    }

    public String key() {
        return "trait.sanctumcraft." + name().toLowerCase();
    }
}

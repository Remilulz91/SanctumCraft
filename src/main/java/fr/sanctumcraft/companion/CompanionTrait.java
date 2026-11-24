package fr.sanctumcraft.companion;

/** Companion traits. Static ones are applied at hatching, dynamic ones every second in the sanctum. */
public enum CompanionTrait {
    /** +25 % attack damage (wolves). */
    FEROCIOUS,
    /** +50 % health. */
    STURDY,
    /** +20 % speed. */
    SWIFT,
    /** Heals its owner (1 heart every 10 s) in the sanctum. */
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

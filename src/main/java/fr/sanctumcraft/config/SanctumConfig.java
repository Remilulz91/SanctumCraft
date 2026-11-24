package fr.sanctumcraft.config;

import fr.sanctumcraft.SanctumCraft;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Simple properties-based config: config/sanctumcraft.properties.
 * A Cloth Config screen will be added later (0.7).
 */
public final class SanctumConfig {

    /** Raid duration in seconds, Normal difficulty. */
    public static int raidDurationSeconds = 1800;
    /** Raid duration in seconds, Nightmare difficulty. */
    public static int raidDurationNightmareSeconds = 1500;
    /** Raid duration in seconds, Chaos difficulty. */
    public static int raidDurationChaosSeconds = 1200;
    /** Cooldown of a portal after its sanctum collapsed, in seconds. */
    public static int portalCooldownSeconds = 1800;
    /** Max different players that can join one sanctum. */
    public static int maxPlayersPerSanctum = 3;
    /** Number of rooms in a sanctum (corridors and great halls count as one room each). */
    public static int roomsPerSanctum = 18;
    /** Number of nest rooms (candidates). */
    public static int nestCandidates = 5;
    /** Min / max number of nests that actually hold an egg. */
    public static int activeNestsMin = 1;
    public static int activeNestsMax = 3;
    /** Hits needed to dig an egg out of a nest. */
    public static int nestHits = 10;
    /** Time to activate the destination mechanism, in seconds. */
    public static int destinationActivationSeconds = 5;
    /** Rooms placed per server tick during generation. */
    public static int roomsPlacedPerTick = 1;
    /** Block positions cleared per server tick when a sanctum collapses. */
    public static int blocksClearedPerTick = 12000;
    /** Minimum time the "Generating..." screen stays visible, in ticks. */
    public static int minGenerationTicks = 40;
    /** Max sanctums existing at the same time on the server. */
    public static int maxSimultaneousSanctums = 16;
    /** Chance (0-100) to add an extra door between two adjacent rooms, creating loops. */
    public static int loopChancePercent = 15;

    /** Players inside a sanctum are put in survival and can't change game mode (except in the dev environment). */
    public static boolean forceSurvival = true;

    /** Players who die inside a sanctum keep their inventory and XP (does not touch the keepInventory gamerule). */
    public static boolean sanctumKeepInventory = false;

    private static final String FILE = "sanctumcraft.properties";

    private SanctumConfig() {}

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE);
        Properties p = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            } catch (Exception e) {
                SanctumCraft.LOGGER.warn("Could not read {}, using defaults", FILE, e);
            }
        }
        raidDurationSeconds = getInt(p, "raidDurationSeconds", raidDurationSeconds, 60, 7200);
        raidDurationNightmareSeconds = getInt(p, "raidDurationNightmareSeconds", raidDurationNightmareSeconds, 60, 7200);
        raidDurationChaosSeconds = getInt(p, "raidDurationChaosSeconds", raidDurationChaosSeconds, 60, 7200);
        portalCooldownSeconds = getInt(p, "portalCooldownSeconds", portalCooldownSeconds, 0, 86400);
        maxPlayersPerSanctum = getInt(p, "maxPlayersPerSanctum", maxPlayersPerSanctum, 1, 16);
        roomsPerSanctum = getInt(p, "roomsPerSanctum", roomsPerSanctum, 6, 60);
        nestCandidates = getInt(p, "nestCandidates", nestCandidates, 1, 12);
        activeNestsMin = getInt(p, "activeNestsMin", activeNestsMin, 1, 12);
        activeNestsMax = getInt(p, "activeNestsMax", activeNestsMax, 1, 12);
        nestHits = getInt(p, "nestHits", nestHits, 1, 100);
        destinationActivationSeconds = getInt(p, "destinationActivationSeconds", destinationActivationSeconds, 0, 60);
        roomsPlacedPerTick = getInt(p, "roomsPlacedPerTick", roomsPlacedPerTick, 1, 10);
        blocksClearedPerTick = getInt(p, "blocksClearedPerTick", blocksClearedPerTick, 1000, 200000);
        minGenerationTicks = getInt(p, "minGenerationTicks", minGenerationTicks, 0, 400);
        maxSimultaneousSanctums = getInt(p, "maxSimultaneousSanctums", maxSimultaneousSanctums, 1, 128);
        loopChancePercent = getInt(p, "loopChancePercent", loopChancePercent, 0, 100);
        forceSurvival = Boolean.parseBoolean(p.getProperty("forceSurvival", String.valueOf(forceSurvival)).trim());
        sanctumKeepInventory = Boolean.parseBoolean(p.getProperty("sanctumKeepInventory", String.valueOf(sanctumKeepInventory)).trim());
        save(path);
    }

    private static int getInt(Properties p, String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void save(Path path) {
        Properties p = new Properties();
        p.setProperty("raidDurationSeconds", String.valueOf(raidDurationSeconds));
        p.setProperty("raidDurationNightmareSeconds", String.valueOf(raidDurationNightmareSeconds));
        p.setProperty("raidDurationChaosSeconds", String.valueOf(raidDurationChaosSeconds));
        p.setProperty("portalCooldownSeconds", String.valueOf(portalCooldownSeconds));
        p.setProperty("maxPlayersPerSanctum", String.valueOf(maxPlayersPerSanctum));
        p.setProperty("roomsPerSanctum", String.valueOf(roomsPerSanctum));
        p.setProperty("nestCandidates", String.valueOf(nestCandidates));
        p.setProperty("activeNestsMin", String.valueOf(activeNestsMin));
        p.setProperty("activeNestsMax", String.valueOf(activeNestsMax));
        p.setProperty("nestHits", String.valueOf(nestHits));
        p.setProperty("destinationActivationSeconds", String.valueOf(destinationActivationSeconds));
        p.setProperty("roomsPlacedPerTick", String.valueOf(roomsPlacedPerTick));
        p.setProperty("blocksClearedPerTick", String.valueOf(blocksClearedPerTick));
        p.setProperty("minGenerationTicks", String.valueOf(minGenerationTicks));
        p.setProperty("maxSimultaneousSanctums", String.valueOf(maxSimultaneousSanctums));
        p.setProperty("loopChancePercent", String.valueOf(loopChancePercent));
        p.setProperty("sanctumKeepInventory", String.valueOf(sanctumKeepInventory));
        p.setProperty("forceSurvival", String.valueOf(forceSurvival));
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                p.store(out, "SanctumCraft configuration (restart the server after editing)");
            }
        } catch (Exception e) {
            SanctumCraft.LOGGER.warn("Could not write {}", FILE, e);
        }
    }
}

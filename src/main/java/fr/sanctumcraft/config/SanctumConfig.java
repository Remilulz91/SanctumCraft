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
 * Also editable in game through Mod Menu when Cloth Config is installed (see SanctumConfigScreen).
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
    public static int roomsPerSanctum = 26;
    /** Number of nest rooms (candidates). */
    public static int nestCandidates = 5;
    /** Min / max number of nests that actually hold an egg. */
    public static int activeNestsMin = 1;
    public static int activeNestsMax = 3;
    /** Hits needed to dig an egg out of a nest. */
    public static int nestHits = 10;
    /** Locked vault rooms per sanctum (opened with the run's key). */
    public static int vaultRooms = 2;
    /** Mobs: density (100 = default), cap per sanctum, rifts per sanctum, trap chance per corridor arm. */
    public static int mobDensityPercent = 100;
    public static int maxMobsPerSanctum = 90;
    public static int riftsPerSanctum = 2;
    public static int trapChancePercent = 25;
    /** Shop: base price of the Chaos Ticket, price multiplier per purchase of the day, reset time zone. */
    public static int chaosTicketPrice = 2500;
    public static double chaosTicketPriceMultiplier = 1.5;
    public static String dailyResetZone = "Europe/Paris";
    /** Incubation time in percent of the default (10 / 20 / 40 / 60 min). */
    public static int incubationTimePercent = 100;
    /** Size of a player's companion collection. */
    public static int companionSlots = 10;
    /** Nobody can (re)enter a sanctum during its last N seconds. */
    public static int reentryLockSeconds = 60;
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
    public static int loopChancePercent = 25;

    /** Players inside a sanctum are put in survival and can't change game mode (except in the dev environment). */
    public static boolean forceSurvival = true;

    /** Players who die inside a sanctum keep their inventory and XP (does not touch the keepInventory gamerule). */
    public static boolean sanctumKeepInventory = false;

    /** Rolling egg: speed bonus in percent, dash cooldown, and whether a player carrying an egg may roll. */
    public static int rollingSpeedPercent = 50;
    public static int rollingDashCooldownSeconds = 3;
    public static boolean rollingWithEggs = false;

    /** Client only: show the sanctum HUD, and its scale in percent. */
    public static boolean hudEnabled = true;
    public static int hudScalePercent = 100;

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
        mobDensityPercent = getInt(p, "mobDensityPercent", mobDensityPercent, 0, 400);
        maxMobsPerSanctum = getInt(p, "maxMobsPerSanctum", maxMobsPerSanctum, 0, 400);
        riftsPerSanctum = getInt(p, "riftsPerSanctum", riftsPerSanctum, 0, 8);
        trapChancePercent = getInt(p, "trapChancePercent", trapChancePercent, 0, 100);
        chaosTicketPrice = getInt(p, "chaosTicketPrice", chaosTicketPrice, 0, 10000000);
        try {
            chaosTicketPriceMultiplier = Math.max(1.0, Double.parseDouble(p.getProperty("chaosTicketPriceMultiplier",
                    String.valueOf(chaosTicketPriceMultiplier)).trim()));
        } catch (NumberFormatException ignored) {
        }
        dailyResetZone = p.getProperty("dailyResetZone", dailyResetZone).trim();
        incubationTimePercent = getInt(p, "incubationTimePercent", incubationTimePercent, 1, 1000);
        companionSlots = getInt(p, "companionSlots", companionSlots, 1, 28);
        reentryLockSeconds = getInt(p, "reentryLockSeconds", reentryLockSeconds, 0, 600);
        vaultRooms = getInt(p, "vaultRooms", vaultRooms, 0, 6);
        nestHits = getInt(p, "nestHits", nestHits, 1, 100);
        destinationActivationSeconds = getInt(p, "destinationActivationSeconds", destinationActivationSeconds, 0, 60);
        roomsPlacedPerTick = getInt(p, "roomsPlacedPerTick", roomsPlacedPerTick, 1, 10);
        blocksClearedPerTick = getInt(p, "blocksClearedPerTick", blocksClearedPerTick, 1000, 200000);
        minGenerationTicks = getInt(p, "minGenerationTicks", minGenerationTicks, 0, 400);
        maxSimultaneousSanctums = getInt(p, "maxSimultaneousSanctums", maxSimultaneousSanctums, 1, 128);
        loopChancePercent = getInt(p, "loopChancePercent", loopChancePercent, 0, 100);
        forceSurvival = Boolean.parseBoolean(p.getProperty("forceSurvival", String.valueOf(forceSurvival)).trim());
        sanctumKeepInventory = Boolean.parseBoolean(p.getProperty("sanctumKeepInventory", String.valueOf(sanctumKeepInventory)).trim());
        rollingSpeedPercent = getInt(p, "rollingSpeedPercent", rollingSpeedPercent, 0, 300);
        rollingDashCooldownSeconds = getInt(p, "rollingDashCooldownSeconds", rollingDashCooldownSeconds, 0, 60);
        rollingWithEggs = Boolean.parseBoolean(p.getProperty("rollingWithEggs", String.valueOf(rollingWithEggs)).trim());
        hudEnabled = Boolean.parseBoolean(p.getProperty("hudEnabled", String.valueOf(hudEnabled)).trim());
        hudScalePercent = getInt(p, "hudScalePercent", hudScalePercent, 50, 200);
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

    /** Writes the current values (used by the config screen). */
    public static void save() {
        save(FabricLoader.getInstance().getConfigDir().resolve(FILE));
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
        p.setProperty("mobDensityPercent", String.valueOf(mobDensityPercent));
        p.setProperty("maxMobsPerSanctum", String.valueOf(maxMobsPerSanctum));
        p.setProperty("riftsPerSanctum", String.valueOf(riftsPerSanctum));
        p.setProperty("trapChancePercent", String.valueOf(trapChancePercent));
        p.setProperty("chaosTicketPrice", String.valueOf(chaosTicketPrice));
        p.setProperty("chaosTicketPriceMultiplier", String.valueOf(chaosTicketPriceMultiplier));
        p.setProperty("dailyResetZone", dailyResetZone);
        p.setProperty("incubationTimePercent", String.valueOf(incubationTimePercent));
        p.setProperty("companionSlots", String.valueOf(companionSlots));
        p.setProperty("reentryLockSeconds", String.valueOf(reentryLockSeconds));
        p.setProperty("vaultRooms", String.valueOf(vaultRooms));
        p.setProperty("nestHits", String.valueOf(nestHits));
        p.setProperty("destinationActivationSeconds", String.valueOf(destinationActivationSeconds));
        p.setProperty("roomsPlacedPerTick", String.valueOf(roomsPlacedPerTick));
        p.setProperty("blocksClearedPerTick", String.valueOf(blocksClearedPerTick));
        p.setProperty("minGenerationTicks", String.valueOf(minGenerationTicks));
        p.setProperty("maxSimultaneousSanctums", String.valueOf(maxSimultaneousSanctums));
        p.setProperty("loopChancePercent", String.valueOf(loopChancePercent));
        p.setProperty("sanctumKeepInventory", String.valueOf(sanctumKeepInventory));
        p.setProperty("forceSurvival", String.valueOf(forceSurvival));
        p.setProperty("rollingSpeedPercent", String.valueOf(rollingSpeedPercent));
        p.setProperty("rollingDashCooldownSeconds", String.valueOf(rollingDashCooldownSeconds));
        p.setProperty("rollingWithEggs", String.valueOf(rollingWithEggs));
        p.setProperty("hudEnabled", String.valueOf(hudEnabled));
        p.setProperty("hudScalePercent", String.valueOf(hudScalePercent));
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

package fr.sanctumcraft.compat;

import fr.sanctumcraft.config.SanctumConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Cloth Config screen over SanctumConfig. Only loaded when Cloth Config is present.
 * In single player the values apply right away; on a dedicated server, edit the server's
 * config/sanctumcraft.properties instead (this screen only edits the local file).
 */
public final class SanctumConfigScreen {

    private SanctumConfigScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.sanctumcraft.title"))
                .setSavingRunnable(SanctumConfig::save);
        ConfigEntryBuilder e = builder.entryBuilder();

        ConfigCategory hud = cat(builder, "hud");
        bool(hud, e, "hudEnabled", SanctumConfig.hudEnabled, true, v -> SanctumConfig.hudEnabled = v);
        integer(hud, e, "hudScalePercent", SanctumConfig.hudScalePercent, 100, 50, 200, v -> SanctumConfig.hudScalePercent = v);

        ConfigCategory raid = cat(builder, "raid");
        integer(raid, e, "raidDurationSeconds", SanctumConfig.raidDurationSeconds, 1800, 60, 7200, v -> SanctumConfig.raidDurationSeconds = v);
        integer(raid, e, "raidDurationNightmareSeconds", SanctumConfig.raidDurationNightmareSeconds, 1500, 60, 7200, v -> SanctumConfig.raidDurationNightmareSeconds = v);
        integer(raid, e, "raidDurationChaosSeconds", SanctumConfig.raidDurationChaosSeconds, 1200, 60, 7200, v -> SanctumConfig.raidDurationChaosSeconds = v);
        integer(raid, e, "portalCooldownSeconds", SanctumConfig.portalCooldownSeconds, 1800, 0, 86400, v -> SanctumConfig.portalCooldownSeconds = v);
        integer(raid, e, "maxPlayersPerSanctum", SanctumConfig.maxPlayersPerSanctum, 3, 1, 16, v -> SanctumConfig.maxPlayersPerSanctum = v);
        integer(raid, e, "reentryLockSeconds", SanctumConfig.reentryLockSeconds, 60, 0, 600, v -> SanctumConfig.reentryLockSeconds = v);
        integer(raid, e, "destinationActivationSeconds", SanctumConfig.destinationActivationSeconds, 5, 0, 60, v -> SanctumConfig.destinationActivationSeconds = v);
        bool(raid, e, "sanctumKeepInventory", SanctumConfig.sanctumKeepInventory, false, v -> SanctumConfig.sanctumKeepInventory = v);
        bool(raid, e, "forceSurvival", SanctumConfig.forceSurvival, true, v -> SanctumConfig.forceSurvival = v);

        ConfigCategory dungeon = cat(builder, "dungeon");
        integer(dungeon, e, "roomsPerSanctum", SanctumConfig.roomsPerSanctum, 26, 6, 60, v -> SanctumConfig.roomsPerSanctum = v);
        integer(dungeon, e, "nestCandidates", SanctumConfig.nestCandidates, 5, 1, 12, v -> SanctumConfig.nestCandidates = v);
        integer(dungeon, e, "activeNestsMin", SanctumConfig.activeNestsMin, 1, 1, 12, v -> SanctumConfig.activeNestsMin = v);
        integer(dungeon, e, "activeNestsMax", SanctumConfig.activeNestsMax, 3, 1, 12, v -> SanctumConfig.activeNestsMax = v);
        integer(dungeon, e, "nestHits", SanctumConfig.nestHits, 10, 1, 100, v -> SanctumConfig.nestHits = v);
        integer(dungeon, e, "vaultRooms", SanctumConfig.vaultRooms, 2, 0, 6, v -> SanctumConfig.vaultRooms = v);
        integer(dungeon, e, "loopChancePercent", SanctumConfig.loopChancePercent, 25, 0, 100, v -> SanctumConfig.loopChancePercent = v);

        ConfigCategory mobs = cat(builder, "mobs");
        integer(mobs, e, "mobDensityPercent", SanctumConfig.mobDensityPercent, 100, 0, 400, v -> SanctumConfig.mobDensityPercent = v);
        integer(mobs, e, "maxMobsPerSanctum", SanctumConfig.maxMobsPerSanctum, 90, 0, 400, v -> SanctumConfig.maxMobsPerSanctum = v);
        integer(mobs, e, "riftsPerSanctum", SanctumConfig.riftsPerSanctum, 2, 0, 8, v -> SanctumConfig.riftsPerSanctum = v);
        integer(mobs, e, "trapChancePercent", SanctumConfig.trapChancePercent, 25, 0, 100, v -> SanctumConfig.trapChancePercent = v);

        ConfigCategory progress = cat(builder, "progress");
        integer(progress, e, "chaosTicketPrice", SanctumConfig.chaosTicketPrice, 2500, 0, 10000000, v -> SanctumConfig.chaosTicketPrice = v);
        progress.addEntry(e.startDoubleField(label("chaosTicketPriceMultiplier"), SanctumConfig.chaosTicketPriceMultiplier)
                .setDefaultValue(1.5).setMin(1.0).setMax(10.0)
                .setTooltip(tip("chaosTicketPriceMultiplier"))
                .setSaveConsumer(v -> SanctumConfig.chaosTicketPriceMultiplier = v).build());
        progress.addEntry(e.startStrField(label("dailyResetZone"), SanctumConfig.dailyResetZone)
                .setDefaultValue("Europe/Paris").setTooltip(tip("dailyResetZone"))
                .setSaveConsumer(v -> SanctumConfig.dailyResetZone = v.trim()).build());
        integer(progress, e, "incubationTimePercent", SanctumConfig.incubationTimePercent, 100, 1, 1000, v -> SanctumConfig.incubationTimePercent = v);
        integer(progress, e, "companionSlots", SanctumConfig.companionSlots, 10, 1, 28, v -> SanctumConfig.companionSlots = v);

        ConfigCategory rolling = cat(builder, "rolling");
        integer(rolling, e, "rollingSpeedPercent", SanctumConfig.rollingSpeedPercent, 50, 0, 300, v -> SanctumConfig.rollingSpeedPercent = v);
        integer(rolling, e, "rollingDashCooldownSeconds", SanctumConfig.rollingDashCooldownSeconds, 3, 0, 60, v -> SanctumConfig.rollingDashCooldownSeconds = v);
        bool(rolling, e, "rollingWithEggs", SanctumConfig.rollingWithEggs, false, v -> SanctumConfig.rollingWithEggs = v);

        ConfigCategory tech = cat(builder, "technical");
        integer(tech, e, "maxSimultaneousSanctums", SanctumConfig.maxSimultaneousSanctums, 16, 1, 128, v -> SanctumConfig.maxSimultaneousSanctums = v);
        integer(tech, e, "roomsPlacedPerTick", SanctumConfig.roomsPlacedPerTick, 1, 1, 10, v -> SanctumConfig.roomsPlacedPerTick = v);
        integer(tech, e, "blocksClearedPerTick", SanctumConfig.blocksClearedPerTick, 12000, 1000, 200000, v -> SanctumConfig.blocksClearedPerTick = v);
        integer(tech, e, "minGenerationTicks", SanctumConfig.minGenerationTicks, 40, 0, 400, v -> SanctumConfig.minGenerationTicks = v);

        return builder.build();
    }

    private static ConfigCategory cat(ConfigBuilder b, String id) {
        return b.getOrCreateCategory(Text.translatable("config.sanctumcraft.category." + id));
    }

    private static Text label(String key) {
        return Text.translatable("config.sanctumcraft." + key);
    }

    private static Text tip(String key) {
        return Text.translatable("config.sanctumcraft." + key + ".tooltip");
    }

    private static void integer(ConfigCategory c, ConfigEntryBuilder e, String key, int value, int def, int min, int max,
                                Consumer<Integer> save) {
        c.addEntry(e.startIntField(label(key), value).setDefaultValue(def).setMin(min).setMax(max)
                .setTooltip(tip(key)).setSaveConsumer(save).build());
    }

    private static void bool(ConfigCategory c, ConfigEntryBuilder e, String key, boolean value, boolean def,
                             Consumer<Boolean> save) {
        c.addEntry(e.startBooleanToggle(label(key), value).setDefaultValue(def)
                .setTooltip(tip(key)).setSaveConsumer(save).build());
    }
}

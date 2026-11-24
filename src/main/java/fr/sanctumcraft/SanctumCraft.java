package fr.sanctumcraft;

import fr.sanctumcraft.command.SanctumCommand;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggHandler;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.KeepInventoryHandler;
import fr.sanctumcraft.instance.SanctumProtection;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class SanctumCraft implements ModInitializer {

    public static final String MOD_ID = "sanctumcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("SanctumCraft");

    /** True when the jar was built with -PbuildType=debug. */
    public static boolean DEBUG_BUILD = false;
    public static String VERSION = "unknown";

    /** Running from IntelliJ / gradlew runClient (never true for a jar in a normal mods folder). */
    public static boolean isDevEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        loadBuildProperties();
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            DEBUG_BUILD = true;
        }
        SanctumConfig.load();

        ModBlocks.register();
        ModItems.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SanctumCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(InstanceManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(InstanceManager::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(InstanceManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(fr.sanctumcraft.profile.SanctumBonuses::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                InstanceManager.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                InstanceManager.onPlayerLeave(handler.player));

        SanctumProtection.register();
        KeepInventoryHandler.register();
        EggHandler.register();

        LOGGER.info("SanctumCraft v{} loaded (build: {})", VERSION, DEBUG_BUILD ? "debug" : "public");
    }

    private static void loadBuildProperties() {
        try (InputStream in = SanctumCraft.class.getResourceAsStream("/sanctumcraft.build.properties")) {
            if (in == null) return;
            Properties p = new Properties();
            p.load(in);
            DEBUG_BUILD = "debug".equalsIgnoreCase(p.getProperty("build_type", "public").trim());
            VERSION = p.getProperty("version", VERSION).trim();
        } catch (Exception e) {
            LOGGER.warn("Could not read build properties", e);
        }
    }
}

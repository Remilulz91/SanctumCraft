package fr.sanctumcraft.client;

import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.net.SanctumNetwork;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.rolling.RollingEgg;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class SanctumCraftClient implements ClientModInitializer {

    private static final String CATEGORY = "key.categories.sanctumcraft";
    private static KeyBinding rollKey;
    private static KeyBinding hudKey;
    private static int welcomeIn;
    private static int welcomeWait;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SANCTUM_PORTAL, RenderLayer.getTranslucent());

        rollKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sanctumcraft.roll", InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_R, CATEGORY));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sanctumcraft.hud", InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_H, CATEGORY));

        ClientPlayNetworking.registerGlobalReceiver(SanctumNetwork.HudPayload.ID, (payload, context) ->
                SanctumHud.onPayload(payload));
        ClientPlayNetworking.registerGlobalReceiver(SanctumNetwork.RollingPayload.ID, (payload, context) -> {
            if (payload.rolling()) RollingEgg.CLIENT_ROLLING.add(payload.player());
            else RollingEgg.CLIENT_ROLLING.remove(payload.player());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> welcomeIn = 60);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SanctumHud.clear();
            RollingEgg.CLIENT_ROLLING.clear();
        });

        HudRenderCallback.EVENT.register(SanctumHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SanctumHud.tick();
            if (welcomeIn > 0 && client.player != null) {
                welcomeIn--;
                // Give the update check a few more seconds if it hasn't answered yet.
                if (welcomeIn == 0 && !fr.sanctumcraft.update.UpdateChecker.done && welcomeWait++ < 200) welcomeIn = 1;
                if (welcomeIn == 0) Welcome.show(client);
            }
            while (rollKey.wasPressed()) {
                if (client.player != null && ClientPlayNetworking.canSend(SanctumNetwork.RollTogglePayload.ID)) {
                    ClientPlayNetworking.send(new SanctumNetwork.RollTogglePayload());
                }
            }
            while (hudKey.wasPressed()) {
                SanctumConfig.hudEnabled = !SanctumConfig.hudEnabled;
                SanctumConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(SanctumConfig.hudEnabled
                            ? "message.sanctumcraft.hud.on" : "message.sanctumcraft.hud.off"), true);
                }
            }
        });
    }

    /** Name of the key bound to sneak (the dash key while rolling). */
    public static Text sneakKeyName() {
        return MinecraftClient.getInstance().options.sneakKey.getBoundKeyLocalizedText();
    }
}

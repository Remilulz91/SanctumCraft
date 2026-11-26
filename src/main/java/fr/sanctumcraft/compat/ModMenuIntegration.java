package fr.sanctumcraft.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Mod Menu entry: opens the SanctumCraft config screen when Cloth Config is installed. */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) return parent -> null;
        return SanctumConfigScreen::create;
    }
}

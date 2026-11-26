package fr.sanctumcraft.client;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.update.UpdateChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

/** Chat message shown when joining a world: what the mod is, guide link, and a newer release if any. */
public final class Welcome {

    private Welcome() {}

    public static void show(MinecraftClient client) {
        if (client.player == null) return;
        String version = SanctumCraft.VERSION.replace("-debug", "");
        if (SanctumConfig.welcomeMessage) {
            client.player.sendMessage(Text.literal("✦ ").formatted(Formatting.DARK_PURPLE)
                    .append(Text.literal("SanctumCraft ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                    .append(Text.literal("v" + version + " ").formatted(Formatting.GRAY))
                    .append(Text.translatable("welcome.sanctumcraft.by", "Remilulz_91").formatted(Formatting.DARK_GRAY)), false);
            client.player.sendMessage(Text.translatable("welcome.sanctumcraft.desc").formatted(Formatting.GRAY), false);
            client.player.sendMessage(Text.translatable("welcome.sanctumcraft.links",
                    link(Text.literal("/sanctum guide"), new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sanctum guide"),
                            Text.translatable("welcome.sanctumcraft.guide_hover")),
                    link(Text.literal("GitHub"), new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/" + UpdateChecker.REPO),
                            Text.literal("github.com/" + UpdateChecker.REPO))).formatted(Formatting.GRAY), false);
        }
        String latest = UpdateChecker.latest;
        if (latest != null) {
            client.player.sendMessage(Text.literal("⬆ ").formatted(Formatting.GOLD)
                    .append(Text.translatable("welcome.sanctumcraft.update", "v" + latest, "v" + version,
                            link(Text.translatable("welcome.sanctumcraft.download"),
                                    new ClickEvent(ClickEvent.Action.OPEN_URL, UpdateChecker.RELEASES_URL),
                                    Text.literal(UpdateChecker.RELEASES_URL))).formatted(Formatting.GOLD)), false);
        }
    }

    private static Text link(MutableText label, ClickEvent click, Text hover) {
        return label.setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true).withClickEvent(click)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }
}

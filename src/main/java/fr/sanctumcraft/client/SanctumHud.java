package fr.sanctumcraft.client;

import fr.sanctumcraft.companion.CompanionSpecies;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggRarity;
import fr.sanctumcraft.instance.SanctumDifficulty;
import fr.sanctumcraft.net.SanctumNetwork.HudPayload;
import fr.sanctumcraft.net.SanctumNetwork.Member;
import fr.sanctumcraft.rolling.RollingEgg;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Sanctum HUD panel (top left): timer, next event, carried eggs, team, companion, rolling egg, Shells. */
public final class SanctumHud {

    private static HudPayload data;
    /** Client tick counter at the time the last snapshot arrived (values are counted down locally). */
    private static long receivedAt;
    private static long clientTicks;

    private SanctumHud() {}

    public static void onPayload(HudPayload payload) {
        data = payload.active() ? payload : null;
        receivedAt = clientTicks;
        RollingEgg.clientLocalRolling = payload.active() && payload.rolling();
    }

    public static void clear() {
        data = null;
        RollingEgg.clientLocalRolling = false;
    }

    public static void tick() {
        clientTicks++;
    }

    public static boolean active() {
        return data != null;
    }

    public static void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudPayload d = data;
        if (d == null || !SanctumConfig.hudEnabled || mc.options.hudHidden || mc.player == null
                || mc.getDebugHud().shouldShowDebugHud()) return;
        long elapsed = clientTicks - receivedAt;
        TextRenderer tr = mc.textRenderer;
        List<Text> lines = new ArrayList<>();

        // Difficulty + timer
        SanctumDifficulty diff = SanctumDifficulty.values()[Math.min(d.difficulty(), SanctumDifficulty.values().length - 1)];
        long remaining = Math.max(0, d.remaining() - elapsed);
        lines.add(Text.translatable(diff.key()).formatted(diff.color, Formatting.BOLD)
                .append(Text.literal("  " + time(remaining)).formatted(remaining <= 1200 ? Formatting.RED : Formatting.WHITE)));

        // Next event
        if (d.nextEvent() > 0) {
            long in = Math.max(0, d.nextEventIn() - elapsed);
            lines.add(Text.translatable("hud.sanctumcraft.event." + d.nextEvent(), time(in)).formatted(Formatting.GOLD));
        } else {
            lines.add(Text.translatable("hud.sanctumcraft.event.final").formatted(Formatting.DARK_RED));
        }

        // Eggs carried
        MutableText eggs = Text.translatable("hud.sanctumcraft.eggs").formatted(Formatting.GRAY);
        boolean any = false;
        for (EggRarity r : EggRarity.values()) {
            int n = d.eggs()[r.ordinal()];
            if (n <= 0) continue;
            eggs.append(Text.literal(any ? ", " : " ").formatted(Formatting.GRAY));
            eggs.append(Text.literal(n + " ").formatted(r.color)).append(Text.translatable(r.key()).formatted(r.color));
            any = true;
        }
        if (!any) eggs.append(Text.literal(" ")).append(Text.translatable("hud.sanctumcraft.none").formatted(Formatting.DARK_GRAY));
        lines.add(eggs);

        // Team
        if (d.team().size() > 1) {
            lines.add(Text.translatable("hud.sanctumcraft.team").formatted(Formatting.GRAY));
            for (Member m : d.team()) {
                MutableText t = Text.literal(" " + m.name() + " ").formatted(Formatting.WHITE);
                switch (m.status()) {
                    case 0 -> t.append(Text.literal("❤ " + Math.round(m.health()) + "/" + Math.round(m.maxHealth()))
                            .formatted(m.health() <= m.maxHealth() * 0.3F ? Formatting.RED : Formatting.GREEN));
                    case 1 -> t.append(Text.translatable("hud.sanctumcraft.dead").formatted(Formatting.DARK_RED));
                    default -> t.append(Text.translatable("hud.sanctumcraft.left").formatted(Formatting.DARK_GRAY));
                }
                lines.add(t);
            }
        }

        // Companion
        if (d.companionSpecies() >= 0 && d.companionSpecies() < CompanionSpecies.values().length) {
            CompanionSpecies sp = CompanionSpecies.values()[d.companionSpecies()];
            EggRarity r = EggRarity.values()[Math.min(d.companionRarity(), 3)];
            MutableText t = Text.translatable("hud.sanctumcraft.companion").formatted(Formatting.GRAY)
                    .append(Text.literal(" ")).append(Text.translatable(sp.key()).formatted(r.color)).append(Text.literal(" "));
            long ko = Math.max(0, d.companionKo() * 20L - elapsed);
            if (d.companionKo() > 0 && ko > 0) {
                t.append(Text.translatable("hud.sanctumcraft.ko", time(ko)).formatted(Formatting.RED));
            } else if (d.companionMax() > 0) {
                t.append(Text.literal("❤ " + Math.round(d.companionHealth()) + "/" + Math.round(d.companionMax()))
                        .formatted(Formatting.GREEN));
            } else {
                t.append(Text.translatable("hud.sanctumcraft.companion_resting").formatted(Formatting.DARK_GRAY));
            }
            lines.add(t);
        }

        // Rolling egg
        if (d.rolling()) {
            long dash = Math.max(0, d.dashCooldown() - elapsed);
            lines.add(Text.translatable("hud.sanctumcraft.rolling").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal(" · ").formatted(Formatting.DARK_GRAY))
                    .append(dash > 0 ? Text.translatable("hud.sanctumcraft.dash_cd", (dash + 19) / 20).formatted(Formatting.GRAY)
                            : Text.translatable("hud.sanctumcraft.dash_ready", SanctumCraftClient.sneakKeyName()).formatted(Formatting.AQUA)));
        }

        // Shells / level
        lines.add(Text.translatable("hud.sanctumcraft.shells", group(d.shells()), d.level()).formatted(Formatting.YELLOW));

        float scale = SanctumConfig.hudScalePercent / 100.0F;
        int width = 0;
        for (Text t : lines) width = Math.max(width, tr.getWidth(t));
        int lineH = tr.fontHeight + 2;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0F);
        int x = 4, y = 4;
        ctx.fill(x - 3, y - 3, x + width + 3, y + lines.size() * lineH + 1, 0x90000000);
        for (Text t : lines) {
            ctx.drawTextWithShadow(tr, t, x, y, 0xFFFFFF);
            y += lineH;
        }
        ctx.getMatrices().pop();
    }

    private static String time(long ticks) {
        long s = (ticks + 19) / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private static String group(long v) {
        String s = Long.toString(Math.abs(v));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append(' ');
            b.append(s.charAt(i));
        }
        return (v < 0 ? "-" : "") + b;
    }
}

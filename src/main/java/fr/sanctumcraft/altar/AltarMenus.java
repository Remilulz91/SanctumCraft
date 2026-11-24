package fr.sanctumcraft.altar;

import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.profile.HunterProfile;
import fr.sanctumcraft.profile.Profiles;
import fr.sanctumcraft.profile.Talent;
import fr.sanctumcraft.companion.CompanionData;
import fr.sanctumcraft.companion.Companions;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.relic.Relic;
import fr.sanctumcraft.relic.RelicItem;
import fr.sanctumcraft.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Altar menus: home, shop, sell, profile. */
public final class AltarMenus {

    private AltarMenus() {}

    private static Text shellsLine(ServerPlayerEntity p) {
        return Text.translatable("menu.sanctumcraft.shells", Profiles.of(p).shells).formatted(Formatting.GOLD);
    }

    // ------------------------------------------------------------------ home

    public static void openMain(ServerPlayerEntity player) {
        HunterProfile prof = Profiles.of(player);
        ButtonMenu.Builder b = new ButtonMenu.Builder(3);
        b.set(4, ButtonMenu.icon(Items.WRITABLE_BOOK, Text.translatable("menu.sanctumcraft.profile").formatted(Formatting.AQUA),
                profileLore(prof)), null);
        b.set(13, ButtonMenu.icon(Items.BONE, Text.translatable("menu.sanctumcraft.companions").formatted(Formatting.GREEN),
                List.of(Text.translatable("menu.sanctumcraft.companions.count", prof.companions.size(),
                        fr.sanctumcraft.config.SanctumConfig.companionSlots).formatted(Formatting.GRAY))), AltarMenus::openCompanions);
        b.set(10, ButtonMenu.icon(Items.EMERALD, Text.translatable("menu.sanctumcraft.shop").formatted(Formatting.GREEN),
                List.of(Text.translatable("menu.sanctumcraft.shop.desc").formatted(Formatting.GRAY))), AltarMenus::openShop);
        b.set(11, ButtonMenu.icon(Items.GOLD_INGOT, Text.translatable("menu.sanctumcraft.sell").formatted(Formatting.GOLD),
                List.of(Text.translatable("menu.sanctumcraft.sell.desc").formatted(Formatting.GRAY))), AltarMenus::openSell);
        b.set(15, ButtonMenu.icon(Items.NETHER_STAR, Text.translatable("menu.sanctumcraft.talents").formatted(Formatting.LIGHT_PURPLE),
                List.of(Text.translatable("menu.sanctumcraft.talents.points", prof.freePoints()).formatted(Formatting.GRAY))),
                AltarMenus::openTalents);
        b.set(16, ButtonMenu.icon(Items.ITEM_FRAME, Text.translatable("menu.sanctumcraft.showcase").formatted(Formatting.YELLOW),
                List.of(Text.translatable("menu.sanctumcraft.showcase.count", prof.showcase.size(), Relic.values().length)
                        .formatted(Formatting.GRAY))), AltarMenus::openShowcase);
        b.set(22, ButtonMenu.icon(Items.SUNFLOWER, shellsLine(player), List.of()), null);
        open(player, b, Text.translatable("menu.sanctumcraft.altar.title"));
    }

    // ------------------------------------------------------------------ talents

    public static void openTalents(ServerPlayerEntity player) {
        HunterProfile prof = Profiles.of(player);
        ButtonMenu.Builder b = new ButtonMenu.Builder(6);
        for (Talent.Branch branch : Talent.Branch.values()) {
            int row = branch.ordinal() + 1;
            b.set(row * 9 + 1, ButtonMenu.icon(branch.icon, Text.translatable(branch.key()).formatted(branch.color, Formatting.BOLD),
                    List.of()), null);
            int col = 3;
            for (Talent t : Talent.values()) {
                if (t.branch != branch) continue;
                int rank = prof.rank(t);
                List<Text> lore = new ArrayList<>();
                lore.add(Text.translatable(t.key() + ".desc").formatted(Formatting.GRAY));
                lore.add(Text.translatable("menu.sanctumcraft.talents.rank", rank, Talent.MAX_RANK)
                        .formatted(rank > 0 ? Formatting.GREEN : Formatting.DARK_GRAY));
                lore.add(Text.translatable(rank >= Talent.MAX_RANK ? "menu.sanctumcraft.talents.max"
                        : "menu.sanctumcraft.talents.learn").formatted(Formatting.DARK_GRAY));
                ItemStack icon = ButtonMenu.icon(t.icon, Text.translatable(t.key()).formatted(branch.color), lore);
                icon.setCount(Math.max(1, rank));
                b.set(row * 9 + col++, icon, p -> {
                    if (Profiles.learn(p, t)) {
                        p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.6F, 1.6F);
                    } else {
                        p.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0F, 1.0F);
                    }
                    openTalents(p);
                });
            }
        }
        b.set(45, ButtonMenu.icon(Items.ARROW, Text.translatable("menu.sanctumcraft.back").formatted(Formatting.GRAY), List.of()),
                AltarMenus::openMain);
        b.set(49, ButtonMenu.icon(Items.NETHER_STAR, Text.translatable("menu.sanctumcraft.talents.points", prof.freePoints())
                .formatted(Formatting.LIGHT_PURPLE), List.of(Text.translatable("menu.sanctumcraft.talents.points_rule")
                .formatted(Formatting.GRAY))), null);
        long price = Profiles.respecPrice(player);
        b.set(53, ButtonMenu.icon(Items.BARRIER, Text.translatable("menu.sanctumcraft.talents.respec").formatted(Formatting.RED),
                List.of(Text.translatable("menu.sanctumcraft.price", price).formatted(Formatting.GOLD))), p -> {
            if (Profiles.respec(p)) {
                p.playSoundToPlayer(SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.MASTER, 1.0F, 1.0F);
            } else {
                p.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0F, 1.0F);
            }
            openTalents(p);
        });
        open(player, b, Text.translatable("menu.sanctumcraft.talents"));
    }

    // ------------------------------------------------------------------ showcase

    public static void openShowcase(ServerPlayerEntity player) {
        HunterProfile prof = Profiles.of(player);
        ButtonMenu.Builder b = new ButtonMenu.Builder(6);
        for (int row = 0; row < 3; row++) {
            int menuRow = row + 1;
            int col = 2;
            for (Relic relic : Relic.values()) {
                if (relic.row != row) continue;
                boolean classic = prof.showcaseClassic.contains(relic.name());
                boolean collector = prof.showcase.contains(relic.name());
                Text name = Text.translatable("item.sanctumcraft." + relic.id());
                ItemStack icon;
                if (collector) {
                    icon = ButtonMenu.icon(RelicItem.collector(new ItemStack(ModItems.RELICS.get(relic))), name.copy().formatted(Formatting.GOLD),
                            List.of(Text.translatable("menu.sanctumcraft.showcase.collector_placed").formatted(Formatting.GOLD)));
                } else if (classic) {
                    icon = ButtonMenu.icon(new ItemStack(ModItems.RELICS.get(relic)), name.copy().formatted(Formatting.YELLOW),
                            List.of(Text.translatable("menu.sanctumcraft.showcase.classic_placed").formatted(Formatting.GREEN),
                                    Text.translatable("menu.sanctumcraft.showcase.add_collector").formatted(Formatting.GRAY)));
                } else {
                    icon = ButtonMenu.icon(Items.GRAY_DYE, name.copy().formatted(Formatting.DARK_GRAY),
                            List.of(Text.translatable("menu.sanctumcraft.showcase.empty").formatted(Formatting.GRAY)));
                }
                b.set(menuRow * 9 + col++, icon, collector ? null : p -> place(p, relic));
            }
            boolean classicRow = Relic.classicRowComplete(prof, row);
            boolean collectorRow = Relic.rowComplete(prof, row);
            b.set(menuRow * 9 + 7, ButtonMenu.icon(collectorRow ? Items.LIME_DYE : classicRow ? Items.YELLOW_DYE : Items.LIGHT_GRAY_DYE,
                    Text.translatable("menu.sanctumcraft.showcase.row" + row).formatted(collectorRow ? Formatting.GREEN : Formatting.GRAY),
                    List.of(Text.translatable("menu.sanctumcraft.showcase.row" + row + ".classic")
                                    .formatted(classicRow ? Formatting.GREEN : Formatting.DARK_GRAY),
                            Text.translatable("menu.sanctumcraft.showcase.row" + row + ".bonus")
                                    .formatted(collectorRow ? Formatting.GREEN : Formatting.DARK_GRAY))), null);
        }
        b.set(45, ButtonMenu.icon(Items.ARROW, Text.translatable("menu.sanctumcraft.back").formatted(Formatting.GRAY), List.of()),
                AltarMenus::openMain);
        b.set(49, ButtonMenu.icon(Items.ITEM_FRAME, Text.translatable("menu.sanctumcraft.showcase.count2",
                prof.showcaseClassic.size(), prof.showcase.size(), Relic.values().length).formatted(Formatting.YELLOW),
                List.of(Text.translatable("menu.sanctumcraft.showcase.rule").formatted(Formatting.GRAY))), null);
        open(player, b, Text.translatable("menu.sanctumcraft.showcase"));
    }

    /** First the classic version, then the collector version on top. */
    private static void place(ServerPlayerEntity player, Relic relic) {
        HunterProfile prof = Profiles.of(player);
        boolean wantCollector = prof.showcaseClassic.contains(relic.name());
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.getItem() instanceof RelicItem ri && ri.relic == relic && RelicItem.isCollector(st) == wantCollector) {
                st.decrement(1);
                (wantCollector ? prof.showcase : prof.showcaseClassic).add(relic.name());
                Profiles.dirty(player);
                player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.8F, wantCollector ? 1.4F : 1.0F);
                openShowcase(player);
                return;
            }
        }
        player.sendMessage(Text.translatable(wantCollector ? "menu.sanctumcraft.showcase.missing"
                : "menu.sanctumcraft.showcase.missing_classic").formatted(Formatting.RED), true);
        player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ companions

    public static void openCompanions(ServerPlayerEntity player) {
        HunterProfile prof = Profiles.of(player);
        ButtonMenu.Builder b = new ButtonMenu.Builder(6);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        long now = player.getServer().getOverworld().getTime();
        for (int i = 0; i < prof.companions.size() && i < slots.length; i++) {
            CompanionData d = prof.companions.get(i);
            boolean active = d.id.equals(prof.activeCompanion);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.translatable("menu.sanctumcraft.companions.stats", pct(d.health), pct(d.damage), pct(d.speed))
                    .formatted(Formatting.GRAY));
            lore.add(Text.translatable("menu.sanctumcraft.companions.traits", Companions.traitsText(d)).formatted(Formatting.AQUA));
            long ko = d.koUntil - now;
            if (ko > 0) {
                lore.add(Text.translatable("menu.sanctumcraft.companions.ko", InstanceManager.formatTime(ko)).formatted(Formatting.RED));
            }
            lore.add(Text.translatable(active ? "menu.sanctumcraft.companions.active" : "menu.sanctumcraft.companions.set_active")
                    .formatted(active ? Formatting.GREEN : Formatting.DARK_GRAY));
            lore.add(Text.translatable("menu.sanctumcraft.companions.release").formatted(Formatting.DARK_RED));
            ItemStack icon = ButtonMenu.icon(new ItemStack(d.species.icon), Companions.describe(d)
                    .append(active ? Text.literal(" ★").formatted(Formatting.YELLOW) : Text.empty()), lore);
            if (active) icon.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            java.util.UUID id = d.id;
            b.set(slots[i], icon, p -> {
                Companions.setActive(p, id);
                openCompanions(p);
            }, p -> {
                if (Companions.requestRelease(p, id)) openCompanions(p);
            });
        }
        b.set(45, ButtonMenu.icon(Items.ARROW, Text.translatable("menu.sanctumcraft.back").formatted(Formatting.GRAY), List.of()),
                AltarMenus::openMain);
        b.set(49, ButtonMenu.icon(ModItems.COMPANION_WHISTLE, Text.translatable("menu.sanctumcraft.companions.count",
                prof.companions.size(), fr.sanctumcraft.config.SanctumConfig.companionSlots).formatted(Formatting.GREEN),
                List.of(Text.translatable("menu.sanctumcraft.companions.help").formatted(Formatting.GRAY))), null);
        open(player, b, Text.translatable("menu.sanctumcraft.companions"));
    }

    private static String pct(double mult) {
        long v = Math.round((mult - 1.0) * 100);
        return (v >= 0 ? "+" : "") + v + " %";
    }

    private static List<Text> profileLore(HunterProfile p) {
        int need = HunterProfile.xpForNext(p.level);
        int filled = Math.round(10f * p.xp / need);
        List<Text> l = new ArrayList<>();
        l.add(Text.translatable("menu.sanctumcraft.profile.level", p.level).formatted(Formatting.YELLOW));
        l.add(Text.literal("■".repeat(filled) + "□".repeat(10 - filled) + "  " + p.xp + " / " + need).formatted(Formatting.GREEN));
        l.add(Text.translatable("menu.sanctumcraft.shells", p.shells).formatted(Formatting.GOLD));
        l.add(Text.translatable("menu.sanctumcraft.profile.stats", p.raids, p.extractions, p.eggs).formatted(Formatting.GRAY));
        return l;
    }

    // ------------------------------------------------------------------ shop

    public static void openShop(ServerPlayerEntity player) {
        ButtonMenu.Builder b = new ButtonMenu.Builder(6);
        List<ShopItem> items = ShopItem.catalogue();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < items.size() && i < slots.length; i++) {
            ShopItem it = items.get(i);
            ItemStack display = it.stack().get();
            ButtonMenu.icon(display, display.getName().copy().formatted(Formatting.WHITE),
                    List.of(Text.translatable("menu.sanctumcraft.price", it.price()).formatted(Formatting.GOLD),
                            Text.translatable("menu.sanctumcraft.click_buy").formatted(Formatting.DARK_GRAY)));
            b.set(slots[i], display, p -> buy(p, it.stack().get(), it.price()));
        }
        long chaosPrice = Profiles.chaosTicketPrice(player);
        b.set(31, ButtonMenu.icon(ModItems.CHAOS_TICKET,
                Text.translatable("item.sanctumcraft.chaos_ticket").formatted(Formatting.DARK_RED, Formatting.BOLD),
                List.of(Text.translatable("menu.sanctumcraft.price", chaosPrice).formatted(Formatting.GOLD),
                        Text.translatable("menu.sanctumcraft.chaos_price_rule",
                                SanctumConfig.chaosTicketPriceMultiplier).formatted(Formatting.GRAY),
                        Text.translatable("menu.sanctumcraft.click_buy").formatted(Formatting.DARK_GRAY))),
                AltarMenus::buyChaosTicket);
        b.set(45, ButtonMenu.icon(Items.ARROW, Text.translatable("menu.sanctumcraft.back").formatted(Formatting.GRAY), List.of()),
                AltarMenus::openMain);
        b.set(49, ButtonMenu.icon(Items.SUNFLOWER, shellsLine(player), List.of()), null);
        open(player, b, Text.translatable("menu.sanctumcraft.shop"));
    }

    private static void buy(ServerPlayerEntity player, ItemStack stack, long price) {
        if (!Profiles.spend(player, price)) {
            player.sendMessage(Text.translatable("menu.sanctumcraft.not_enough", price).formatted(Formatting.RED), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0F, 1.0F);
            return;
        }
        if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
        player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0F, 1.0F);
        openShop(player); // refresh balance
    }

    private static void buyChaosTicket(ServerPlayerEntity player) {
        long price = Profiles.chaosTicketPrice(player);
        if (!Profiles.spend(player, price)) {
            player.sendMessage(Text.translatable("menu.sanctumcraft.not_enough", price).formatted(Formatting.RED), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0F, 1.0F);
            return;
        }
        Profiles.chaosTicketBought(player);
        ItemStack ticket = new ItemStack(ModItems.CHAOS_TICKET);
        if (!player.getInventory().insertStack(ticket)) player.dropItem(ticket, false);
        player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0F, 0.8F);
        openShop(player);
    }

    // ------------------------------------------------------------------ sell

    public static void openSell(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new SellMenu(syncId, inv, player), Text.translatable("menu.sanctumcraft.sell")));
    }

    private static void open(ServerPlayerEntity player, ButtonMenu.Builder b, Text title) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> b.build(syncId, inv), title));
    }
}

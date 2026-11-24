package fr.sanctumcraft.altar;

import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.profile.HunterProfile;
import fr.sanctumcraft.profile.Profiles;
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
        b.set(11, ButtonMenu.icon(Items.EMERALD, Text.translatable("menu.sanctumcraft.shop").formatted(Formatting.GREEN),
                List.of(Text.translatable("menu.sanctumcraft.shop.desc").formatted(Formatting.GRAY))), AltarMenus::openShop);
        b.set(13, ButtonMenu.icon(Items.GOLD_INGOT, Text.translatable("menu.sanctumcraft.sell").formatted(Formatting.GOLD),
                List.of(Text.translatable("menu.sanctumcraft.sell.desc").formatted(Formatting.GRAY))), AltarMenus::openSell);
        b.set(15, ButtonMenu.icon(Items.WRITABLE_BOOK, Text.translatable("menu.sanctumcraft.profile").formatted(Formatting.AQUA),
                profileLore(prof)), null);
        b.set(22, ButtonMenu.icon(Items.SUNFLOWER, shellsLine(player), List.of()), null);
        open(player, b, Text.translatable("menu.sanctumcraft.altar.title"));
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

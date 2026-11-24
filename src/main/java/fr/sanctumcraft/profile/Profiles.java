package fr.sanctumcraft.profile;

import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggRarity;
import fr.sanctumcraft.instance.SanctumDifficulty;
import fr.sanctumcraft.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Shells, Hunter levels and stats. */
public final class Profiles {

    private Profiles() {}

    public static HunterProfile of(ServerPlayerEntity player) {
        return ProfileState.get(player.getServer()).of(player.getUuid());
    }

    public static void dirty(ServerPlayerEntity player) {
        ProfileState.get(player.getServer()).markDirty();
    }

    public static int eggShells(EggRarity r) {
        return switch (r) {
            case COMMON -> 50;
            case RARE -> 120;
            case EPIC -> 300;
            case LEGENDARY -> 800;
        };
    }

    public static int eggHunterXp(EggRarity r) {
        return switch (r) {
            case COMMON -> 25;
            case RARE -> 60;
            case EPIC -> 140;
            case LEGENDARY -> 350;
        };
    }

    public static void onRaidJoined(ServerPlayerEntity player) {
        of(player).raids++;
        dirty(player);
    }

    /** Extraction: shells + Hunter XP for every newly secured egg. Returns the shells earned. */
    public static long onExtraction(ServerPlayerEntity player, List<EggRarity> eggs, SanctumDifficulty difficulty) {
        HunterProfile p = of(player);
        long shells = 0;
        int xp = 0;
        double bonus = shellBonus(p);
        for (EggRarity r : eggs) {
            shells += Math.round(eggShells(r) * difficulty.loot * bonus);
            xp += Math.round(eggHunterXp(r) * difficulty.loot);
        }
        p.shells += shells;
        p.extractions++;
        p.eggs += eggs.size();
        dirty(player);
        addHunterXp(player, xp);
        return shells;
    }

    /** Shells multiplier: Greed talent, +2 % per showcased relic, +25 % for the complete "Nest" row. */
    public static double shellBonus(HunterProfile p) {
        double b = 1.0 + 0.10 * p.rank(Talent.GREED) + 0.02 * p.showcase.size();
        if (fr.sanctumcraft.relic.Relic.rowComplete(p, 2)) b += 0.25;
        return b;
    }

    public static int rank(ServerPlayerEntity player, Talent t) {
        return of(player).rank(t);
    }

    public static boolean learn(ServerPlayerEntity player, Talent t) {
        HunterProfile p = of(player);
        if (p.freePoints() <= 0 || p.rank(t) >= Talent.MAX_RANK) return false;
        p.talents.put(t.name(), p.rank(t) + 1);
        dirty(player);
        return true;
    }

    public static long respecPrice(ServerPlayerEntity player) {
        return 300L * of(player).level;
    }

    public static boolean respec(ServerPlayerEntity player) {
        HunterProfile p = of(player);
        if (p.talents.isEmpty() || !spend(player, respecPrice(player))) return false;
        p.talents.clear();
        dirty(player);
        return true;
    }

    public static void addShells(ServerPlayerEntity player, long amount) {
        of(player).shells += amount;
        dirty(player);
    }

    public static boolean spend(ServerPlayerEntity player, long amount) {
        HunterProfile p = of(player);
        if (p.shells < amount) return false;
        p.shells -= amount;
        dirty(player);
        return true;
    }

    /** Adds Hunter XP; every level gives a Chaos Ticket and shells. */
    public static void addHunterXp(ServerPlayerEntity player, int amount) {
        HunterProfile p = of(player);
        p.xp += amount;
        while (p.xp >= HunterProfile.xpForNext(p.level)) {
            p.xp -= HunterProfile.xpForNext(p.level);
            p.level++;
            long bonus = 50L * p.level;
            p.shells += bonus;
            ItemStack ticket = new ItemStack(ModItems.CHAOS_TICKET);
            if (!player.getInventory().insertStack(ticket)) player.dropItem(ticket, false);
            player.sendMessage(Text.translatable("message.sanctumcraft.level_up", p.level, bonus).formatted(Formatting.GOLD));
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0F, 1.2F);
        }
        dirty(player);
    }

    // ------------------------------------------------------------------ Chaos ticket price

    public static String today() {
        ZoneId zone;
        try {
            zone = ZoneId.of(SanctumConfig.dailyResetZone);
        } catch (Exception e) {
            zone = ZoneId.of("Europe/Paris");
        }
        return LocalDate.now(zone).toString();
    }

    public static long chaosTicketPrice(ServerPlayerEntity player) {
        HunterProfile p = of(player);
        int bought = today().equals(p.chaosDay) ? p.chaosBought : 0;
        return Math.round(SanctumConfig.chaosTicketPrice * Math.pow(SanctumConfig.chaosTicketPriceMultiplier, bought));
    }

    public static void chaosTicketBought(ServerPlayerEntity player) {
        HunterProfile p = of(player);
        String day = today();
        if (!day.equals(p.chaosDay)) {
            p.chaosDay = day;
            p.chaosBought = 0;
        }
        p.chaosBought++;
        dirty(player);
    }
}

package fr.sanctumcraft.hud;

import fr.sanctumcraft.companion.CompanionData;
import fr.sanctumcraft.companion.Companions;
import fr.sanctumcraft.egg.SanctumEggItem;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.net.SanctumNetwork;
import fr.sanctumcraft.net.SanctumNetwork.HudPayload;
import fr.sanctumcraft.net.SanctumNetwork.Member;
import fr.sanctumcraft.profile.HunterProfile;
import fr.sanctumcraft.profile.Profiles;
import fr.sanctumcraft.rolling.RollingEgg;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/** Sends the sanctum HUD snapshot to every player inside an active sanctum, once per second. */
public final class HudSync {

    /** Players whose HUD is currently shown (so it can be cleared when they leave). */
    private static final Set<UUID> SHOWN = new HashSet<>();

    private HudSync() {}

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        Set<UUID> now = new HashSet<>();
        for (SanctumInstance inst : InstanceManager.instances(server)) {
            if (inst.status != SanctumInstance.Status.ACTIVE) continue;
            List<ServerPlayerEntity> inside = InstanceManager.playersInside(server, inst);
            for (ServerPlayerEntity p : inside) {
                SanctumNetwork.send(p, build(server, inst, p, inside));
                now.add(p.getUuid());
            }
        }
        for (UUID id : SHOWN) {
            if (now.contains(id)) continue;
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) SanctumNetwork.send(p, HudPayload.inactive());
        }
        SHOWN.clear();
        SHOWN.addAll(now);
    }

    /** Immediate refresh for one player (rolling started / stopped...). */
    public static void refresh(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SanctumInstance inst = InstanceManager.instanceOf(player);
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) return;
        SanctumNetwork.send(player, build(server, inst, player, InstanceManager.playersInside(server, inst)));
        SHOWN.add(player.getUuid());
    }

    public static void forget(ServerPlayerEntity player) {
        SHOWN.remove(player.getUuid());
    }

    private static HudPayload build(MinecraftServer server, SanctumInstance inst, ServerPlayerEntity player,
                                    List<ServerPlayerEntity> inside) {
        long remaining = Math.max(0, InstanceManager.remainingTicks(server, inst));
        long total = inst.difficulty.durationSeconds() * 20L;
        long elapsed = total - remaining;

        int nextEvent = 0;
        long nextIn = 0;
        switch (inst.waveStage) {
            case 0 -> {
                nextEvent = 1;
                nextIn = total * 3 / 10 - elapsed;
            }
            case 1 -> {
                nextEvent = 2;
                nextIn = remaining - 300 * 20L;
            }
            case 2 -> {
                nextEvent = 3;
                nextIn = remaining - 120 * 20L;
            }
            default -> {
            }
        }

        int[] eggs = new int[4];
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.getItem() instanceof SanctumEggItem egg && !SanctumEggItem.isSecured(s)) {
                eggs[egg.rarity.ordinal()] += s.getCount();
            }
        }

        List<Member> team = new ArrayList<>();
        for (UUID id : inst.members) {
            if (team.size() >= 6) break;
            ServerPlayerEntity m = server.getPlayerManager().getPlayer(id);
            if (m == null) continue;
            int status;
            if (!inside.contains(m)) status = 2;
            else if (!m.isAlive() || m.isSpectator()) status = 1;
            else status = 0;
            team.add(new Member(m.getName().getString(), status, m.getHealth(), m.getMaxHealth()));
        }

        HunterProfile prof = Profiles.of(player);
        int species = -1, rarity = 0, ko = 0;
        float ch = 0, cm = 0;
        CompanionData d = Companions.activeData(player);
        if (d != null) {
            species = d.species.ordinal();
            rarity = d.rarity.ordinal();
            long koLeft = d.koUntil - server.getOverworld().getTime();
            if (koLeft > 0) {
                ko = (int) ((koLeft + 19) / 20);
            } else {
                MobEntity mob = Companions.summoned(player);
                if (mob != null) {
                    ch = mob.getHealth();
                    cm = mob.getMaxHealth();
                }
            }
        }

        return new HudPayload(true, inst.difficulty.ordinal(), (int) remaining, (int) total, nextEvent,
                (int) Math.max(0, nextIn), eggs, prof.shells, prof.level, team, species, rarity, ch, cm, ko,
                RollingEgg.isRolling(player), RollingEgg.dashCooldownTicks(player));
    }
}

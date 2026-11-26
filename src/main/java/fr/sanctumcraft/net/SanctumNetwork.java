package fr.sanctumcraft.net;

import fr.sanctumcraft.SanctumCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Custom packets: HUD state (server -> client), rolling egg state (both ways). */
public final class SanctumNetwork {

    private SanctumNetwork() {}

    /** One teammate line of the HUD. status: 0 alive inside, 1 dead, 2 outside / offline. */
    public record Member(String name, int status, float health, float maxHealth) {}

    /**
     * Full HUD snapshot, sent every second to players inside a sanctum (active = false clears the HUD).
     * nextEvent: 0 none, 1 early reinforcements, 2 stronger monsters (+25 %), 3 stronger monsters (+50 %).
     * companionSpecies = -1 when no active companion; companionKo = KO seconds left (0 = standing).
     */
    public record HudPayload(boolean active, int difficulty, int remaining, int total, int nextEvent, int nextEventIn,
                             int[] eggs, long shells, int level, List<Member> team,
                             int companionSpecies, int companionRarity, float companionHealth, float companionMax,
                             int companionKo, boolean rolling, int dashCooldown) implements CustomPayload {

        public static final Id<HudPayload> ID = new Id<>(SanctumCraft.id("hud"));
        public static final PacketCodec<RegistryByteBuf, HudPayload> CODEC = PacketCodec.of(HudPayload::write, HudPayload::read);

        public static HudPayload inactive() {
            return new HudPayload(false, 0, 0, 1, 0, 0, new int[4], 0, 1, List.of(), -1, 0, 0, 0, 0, false, 0);
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeVarInt(difficulty);
            buf.writeVarInt(remaining);
            buf.writeVarInt(total);
            buf.writeVarInt(nextEvent);
            buf.writeVarInt(nextEventIn);
            for (int i = 0; i < 4; i++) buf.writeVarInt(eggs[i]);
            buf.writeVarLong(shells);
            buf.writeVarInt(level);
            buf.writeVarInt(team.size());
            for (Member m : team) {
                buf.writeString(m.name());
                buf.writeVarInt(m.status());
                buf.writeFloat(m.health());
                buf.writeFloat(m.maxHealth());
            }
            buf.writeVarInt(companionSpecies + 1);
            buf.writeVarInt(companionRarity);
            buf.writeFloat(companionHealth);
            buf.writeFloat(companionMax);
            buf.writeVarInt(companionKo);
            buf.writeBoolean(rolling);
            buf.writeVarInt(dashCooldown);
        }

        private static HudPayload read(RegistryByteBuf buf) {
            boolean active = buf.readBoolean();
            int difficulty = buf.readVarInt();
            int remaining = buf.readVarInt();
            int total = buf.readVarInt();
            int nextEvent = buf.readVarInt();
            int nextEventIn = buf.readVarInt();
            int[] eggs = new int[4];
            for (int i = 0; i < 4; i++) eggs[i] = buf.readVarInt();
            long shells = buf.readVarLong();
            int level = buf.readVarInt();
            int n = Math.min(16, buf.readVarInt());
            List<Member> team = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                team.add(new Member(buf.readString(64), buf.readVarInt(), buf.readFloat(), buf.readFloat()));
            }
            int species = buf.readVarInt() - 1;
            int rarity = buf.readVarInt();
            float ch = buf.readFloat();
            float cm = buf.readFloat();
            int ko = buf.readVarInt();
            boolean rolling = buf.readBoolean();
            int dash = buf.readVarInt();
            return new HudPayload(active, difficulty, remaining, total, nextEvent, nextEventIn, eggs, shells, level, team,
                    species, rarity, ch, cm, ko, rolling, dash);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Tells clients that a player started / stopped rolling (they draw an egg instead of the player). */
    public record RollingPayload(UUID player, boolean rolling) implements CustomPayload {
        public static final Id<RollingPayload> ID = new Id<>(SanctumCraft.id("rolling"));
        public static final PacketCodec<RegistryByteBuf, RollingPayload> CODEC = PacketCodec.of(
                (p, buf) -> {
                    buf.writeUuid(p.player());
                    buf.writeBoolean(p.rolling());
                },
                buf -> new RollingPayload(buf.readUuid(), buf.readBoolean()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Client asks to toggle the rolling egg (key press). */
    public record RollTogglePayload() implements CustomPayload {
        public static final Id<RollTogglePayload> ID = new Id<>(SanctumCraft.id("roll_toggle"));
        public static final PacketCodec<RegistryByteBuf, RollTogglePayload> CODEC = PacketCodec.of(
                (p, buf) -> {
                }, buf -> new RollTogglePayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(HudPayload.ID, HudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RollingPayload.ID, RollingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RollTogglePayload.ID, RollTogglePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RollTogglePayload.ID, (payload, context) ->
                fr.sanctumcraft.rolling.RollingEgg.toggle(context.player()));
    }

    public static void send(ServerPlayerEntity player, CustomPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.getId())) ServerPlayNetworking.send(player, payload);
    }
}

package fr.sanctumcraft.mob;

import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.PlacedRoom;
import fr.sanctumcraft.instance.RoomKind;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.loot.ChestTier;
import fr.sanctumcraft.loot.SanctumChestBlock;
import fr.sanctumcraft.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.*;

/** Rift events: 3 waves of monsters around the rift; when all are dead, a chest appears. */
public final class RiftManager {

    private static final int WAVES = 3;
    private static final int WAVE_INTERVAL = 200; // 10 s

    /** Runtime state of a triggered rift. */
    public static final class RiftEvent {
        int wave;
        long nextWave;
        final Set<UUID> mobs = new HashSet<>();
    }

    private RiftManager() {}

    /** Places the rifts of a sanctum (in halls / great halls, near the room centre). */
    public static void placeRifts(ServerWorld world, SanctumInstance inst) {
        Random random = world.getRandom();
        List<PlacedRoom> candidates = new ArrayList<>();
        for (PlacedRoom r : inst.rooms) {
            if (r.kind() == RoomKind.HALL || r.kind() == RoomKind.GREAT_HALL) candidates.add(r);
        }
        Collections.shuffle(candidates, new java.util.Random(random.nextLong()));
        int count = Math.min(SanctumConfig.riftsPerSanctum, candidates.size());
        for (int i = 0; i < count; i++) {
            PlacedRoom room = candidates.get(i);
            BlockPos center = inst.roomOrigin(room).add(room.w() * 25 / 2, 1, room.h() * 25 / 2);
            BlockPos spot = freeFloor(world, center);
            if (spot != null) world.setBlockState(spot, ModBlocks.RIFT.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static BlockPos freeFloor(ServerWorld world, BlockPos center) {
        for (int r = 0; r <= 7; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos p = center.add(dx, 0, dz);
                    if (world.getBlockState(p).isAir() && world.getBlockState(p.up()).isAir()
                            && world.getBlockState(p.down()).isSideSolidFullSquare(world, p.down(), Direction.UP)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public static void start(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
        SanctumInstance inst = InstanceManager.instanceAt(world, pos);
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) return;
        if (inst.rifts.containsKey(pos.asLong())) return;
        RiftEvent ev = new RiftEvent();
        ev.nextWave = world.getTime();
        inst.rifts.put(pos.asLong(), ev);
        world.setBlockState(pos, world.getBlockState(pos).with(RiftBlock.ACTIVE, true), Block.NOTIFY_LISTENERS);
        world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 1.0F, 1.3F);
        for (ServerPlayerEntity p : InstanceManager.playersInside(world.getServer(), inst)) {
            title(p, Text.empty(), Text.translatable("subtitle.sanctumcraft.rift.opened").formatted(Formatting.DARK_PURPLE));
        }
    }

    /** Called every tick for an active sanctum. */
    public static void tick(ServerWorld world, SanctumInstance inst) {
        if (inst.rifts.isEmpty()) return;
        long now = world.getTime();
        for (Iterator<Map.Entry<Long, RiftEvent>> it = inst.rifts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, RiftEvent> e = it.next();
            BlockPos pos = BlockPos.fromLong(e.getKey());
            RiftEvent ev = e.getValue();
            if (ev.wave < WAVES && now >= ev.nextWave) {
                ev.wave++;
                ev.nextWave = now + WAVE_INTERVAL;
                int count = Math.round((2 + ev.wave) * inst.difficulty.mobCount);
                for (int i = 0; i < count; i++) {
                    MobEntity mob = SanctumMobs.spawn(world, inst, pos, 5);
                    if (mob != null) ev.mobs.add(mob.getUuid());
                }
                world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.0F, 0.8F);
                continue;
            }
            if (now % 20 != 0) continue;
            ev.mobs.removeIf(id -> {
                Entity ent = world.getEntity(id);
                return ent == null || !ent.isAlive();
            });
            if (ev.wave >= WAVES && ev.mobs.isEmpty()) {
                reward(world, inst, pos);
                it.remove();
            }
        }
    }

    private static void reward(ServerWorld world, SanctumInstance inst, BlockPos pos) {
        ChestTier tier = ChestTier.roll(world.getRandom(), inst.difficulty).bump(1);
        if (tier.ordinal() < ChestTier.RARE.ordinal()) tier = ChestTier.RARE;
        BlockState chest = ModBlocks.chestFor(tier).getDefaultState()
                .with(SanctumChestBlock.FACING, Direction.Type.HORIZONTAL.random(world.getRandom()));
        world.setBlockState(pos, chest, Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        for (ServerPlayerEntity p : InstanceManager.playersInside(world.getServer(), inst)) {
            title(p, Text.empty(), Text.translatable("subtitle.sanctumcraft.rift.cleared").formatted(Formatting.GOLD));
        }
    }

    private static void title(ServerPlayerEntity p, Text title, Text subtitle) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        p.networkHandler.sendPacket(new TitleS2CPacket(title));
    }
}

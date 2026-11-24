package fr.sanctumcraft.instance;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.block.DestinationMechanismBlock;
import fr.sanctumcraft.block.EggNestBlock;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggHandler;
import fr.sanctumcraft.portal.SanctumPortalBlock;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.menu.DifficultyMenu;
import fr.sanctumcraft.registry.ModItems;
import fr.sanctumcraft.portal.PortalHandler;
import fr.sanctumcraft.portal.PortalShape;
import fr.sanctumcraft.registry.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.*;

/**
 * Lifecycle of portals and sanctums:
 * portal contact -> GENERATING (players frozen, "Generating...") -> ACTIVE (timer, bossbar)
 * -> timer reaches zero: players inside die -> CLOSING (blocks cleared over several ticks)
 * -> removed, portal cooldown starts.
 */
public final class InstanceManager {

    /** Distance between two sanctum slots along X, in blocks. */
    private static final int SLOT_SPACING = 512;
    private static final int BASE_Y = 64;

    private static final BlockState SEAL = Blocks.DEEPSLATE_BRICKS.getDefaultState();
    /** Position of the nest block in a nest room, of the mechanism in the destination room. */
    private static final BlockPos NEST_OFFSET = new BlockPos(12, 3, 12);
    private static final BlockPos MECHANISM_OFFSET = new BlockPos(12, 3, 12);
    /** Exit portal of the destination room: interior 3 wide (x 11..13) x 4 tall (y 2..5) at z = 20. */
    private static final BlockPos EXIT_FRAME_OFFSET = new BlockPos(11, 2, 20);
    private static final int CLEAR_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;

    private InstanceManager() {}

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private static SanctumState state(MinecraftServer server) {
        return SanctumState.get(server);
    }

    private static long now(MinecraftServer server) {
        return server.getOverworld().getTime();
    }

    private static ServerWorld sanctumWorld(MinecraftServer server) {
        return server.getWorld(ModDimensions.SANCTUM);
    }

    private static String dimId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    public static String formatTime(long ticks) {
        long seconds = Math.max(0, (ticks + 19) / 20);
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static void title(ServerPlayerEntity p, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        p.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    /** Online players currently standing inside the given sanctum. */
    public static List<ServerPlayerEntity> playersInside(MinecraftServer server, SanctumInstance inst) {
        ServerWorld world = sanctumWorld(server);
        List<ServerPlayerEntity> list = new ArrayList<>();
        if (world == null) return list;
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (inst.contains(p.getBlockPos())) list.add(p);
        }
        return list;
    }

    public static SanctumInstance instanceAt(ServerWorld world, BlockPos pos) {
        if (!ModDimensions.isSanctum(world)) return null;
        return state(world.getServer()).findInstanceAt(pos);
    }

    public static SanctumInstance instanceOf(ServerPlayerEntity player) {
        if (player.getServer() == null || !ModDimensions.isSanctum(player.getWorld())) return null;
        return state(player.getServer()).findInstanceAt(player.getBlockPos());
    }

    // ------------------------------------------------------------------------------------------
    // Server lifecycle
    // ------------------------------------------------------------------------------------------

    public static void onServerStarted(MinecraftServer server) {
        if (sanctumWorld(server) == null) {
            SanctumCraft.LOGGER.error("Sanctum dimension not found! Is the datapack part of the mod jar intact?");
        }
        SanctumState state = state(server);
        for (SanctumInstance inst : state.instances.values()) {
            if (inst.status == SanctumInstance.Status.GENERATING) {
                // Generation was interrupted: throw it away.
                startClosing(server, inst, false);
            }
        }
        // Drop links to sanctums that no longer exist.
        for (PortalRecord r : state.portals.values()) {
            if (r.activeInstance != null && !state.instances.containsKey(r.activeInstance)) {
                r.activeInstance = null;
            }
        }
        state.markDirty();
    }

    public static void onServerStopping(MinecraftServer server) {
        for (SanctumInstance inst : state(server).instances.values()) {
            if (inst.bossBar != null) inst.bossBar.clearPlayers();
            inst.bossBar = null;
            inst.waiting.clear();
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (!ModDimensions.isSanctum(player.getWorld()) || player.getServer() == null) return;
        SanctumInstance inst = state(player.getServer()).findInstanceAt(player.getBlockPos());
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) {
            // The sanctum collapsed while this player was offline.
            player.sendMessage(Text.translatable("message.sanctumcraft.collapsed_offline").formatted(Formatting.DARK_RED));
            if (player.isSpectator()) {
                sendToSpawn(player);
            } else {
                player.kill();
            }
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        if (player.getServer() == null) return;
        for (SanctumInstance inst : state(player.getServer()).instances.values()) {
            inst.waiting.remove(player.getUuid());
            if (inst.bossBar != null) inst.bossBar.removePlayer(player);
        }
        PortalHandler.forget(player.getUuid());
    }

    // ------------------------------------------------------------------------------------------
    // Portals
    // ------------------------------------------------------------------------------------------

    public static void registerPortal(ServerWorld world, PortalShape shape) {
        SanctumState state = state(world.getServer());
        PortalRecord record = new PortalRecord(UUID.randomUUID(), dimId(world), shape.box());
        state.portals.put(record.id, record);
        state.markDirty();
    }

    public static void onPortalBlockRemoved(ServerWorld world, BlockPos pos) {
        SanctumState state = state(world.getServer());
        PortalRecord r = state.findPortal(dimId(world), pos);
        if (r != null) {
            // The running sanctum (if any) keeps going; players get back to their return point.
            state.portals.remove(r.id);
            state.markDirty();
        }
    }

    public static void enterPortal(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        SanctumState state = state(server);
        PortalRecord portal = state.findPortal(dimId(world), pos);
        if (portal == null) {
            player.sendMessage(Text.translatable("message.sanctumcraft.portal.unlinked").formatted(Formatting.GRAY), true);
            return;
        }

        if (portal.activeInstance != null) {
            SanctumInstance inst = state.instances.get(portal.activeInstance);
            if (inst != null && inst.status != SanctumInstance.Status.CLOSING) {
                if (!inst.members.contains(player.getUuid()) && inst.members.size() >= SanctumConfig.maxPlayersPerSanctum) {
                    player.sendMessage(Text.translatable("message.sanctumcraft.full",
                            SanctumConfig.maxPlayersPerSanctum).formatted(Formatting.RED), true);
                    return;
                }
                join(server, inst, player);
                return;
            }
            portal.activeInstance = null;
        }

        long remaining = portal.cooldownUntil - now(server);
        if (remaining > 0) {
            player.sendMessage(Text.translatable("message.sanctumcraft.cooldown", formatTime(remaining))
                    .formatted(Formatting.GOLD), true);
            return;
        }

        // Free portal: the player picks a difficulty first.
        DifficultyMenu.open(player, portal.id);
    }

    /** Called by the difficulty menu. Re-checks everything: the world may have changed meanwhile. */
    public static void openFromPortal(ServerPlayerEntity player, UUID portalId, SanctumDifficulty difficulty) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SanctumState state = state(server);
        PortalRecord portal = state.portals.get(portalId);
        if (portal == null || !portal.dimension.equals(dimId(player.getWorld()))
                || portal.box.getCenter().getSquaredDistance(player.getPos()) > 36) {
            player.sendMessage(Text.translatable("message.sanctumcraft.portal.too_far").formatted(Formatting.RED), true);
            return;
        }
        if (portal.activeInstance != null && state.instances.containsKey(portal.activeInstance)) {
            // Someone opened it in the meantime: join theirs.
            SanctumInstance inst = state.instances.get(portal.activeInstance);
            if (inst.status != SanctumInstance.Status.CLOSING
                    && (inst.members.contains(player.getUuid()) || inst.members.size() < SanctumConfig.maxPlayersPerSanctum)) {
                join(server, inst, player);
            }
            return;
        }
        long remaining = portal.cooldownUntil - now(server);
        if (remaining > 0) {
            player.sendMessage(Text.translatable("message.sanctumcraft.cooldown", formatTime(remaining)).formatted(Formatting.GOLD), true);
            return;
        }
        if (difficulty == SanctumDifficulty.CHAOS && !consumeChaosTicket(player)) {
            player.sendMessage(Text.translatable("message.sanctumcraft.chaos_locked").formatted(Formatting.RED), true);
            return;
        }
        SanctumInstance inst = create(server, portal.id, difficulty);
        if (inst == null) {
            player.sendMessage(Text.translatable("message.sanctumcraft.no_slot").formatted(Formatting.RED), true);
            return;
        }
        portal.activeInstance = inst.id;
        state.markDirty();
        join(server, inst, player);
    }

    public static boolean hasChaosTicket(ServerPlayerEntity player) {
        return player.getAbilities().creativeMode || player.getInventory().count(ModItems.CHAOS_TICKET) > 0;
    }

    private static boolean consumeChaosTicket(ServerPlayerEntity player) {
        if (player.getAbilities().creativeMode) return true;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ModItems.CHAOS_TICKET)) {
                inv.getStack(i).decrement(1);
                return true;
            }
        }
        return false;
    }

    /** Adds a player to a sanctum: frozen while generating, teleported in when active. */
    private static void join(MinecraftServer server, SanctumInstance inst, ServerPlayerEntity player) {
        inst.members.add(player.getUuid());
        inst.returns.put(player.getUuid(), ReturnPoint.of(player));
        state(server).markDirty();
        if (inst.status == SanctumInstance.Status.GENERATING) {
            inst.waiting.put(player.getUuid(), player.getPos());
            title(player, Text.translatable("title.sanctumcraft.generating").formatted(Formatting.LIGHT_PURPLE),
                    Text.translatable("subtitle.sanctumcraft.generating", inst.roomsPlaced, inst.rooms.size())
                            .formatted(Formatting.GRAY), 5, 40, 5);
        } else if (inst.status == SanctumInstance.Status.ACTIVE) {
            teleportIn(server, inst, player);
        }
    }

    private static void teleportIn(MinecraftServer server, SanctumInstance inst, ServerPlayerEntity player) {
        ServerWorld world = sanctumWorld(server);
        if (world == null || inst.spawn == null) return;
        if (SanctumConfig.forceSurvival && player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
            player.changeGameMode(GameMode.SURVIVAL);
        }
        player.teleport(world, inst.spawn.getX() + 0.5, inst.spawn.getY(), inst.spawn.getZ() + 0.5, inst.spawnYaw, 0.0F);
        PortalHandler.markContact(player);
        long remaining = inst.endTick - now(server);
        title(player, Text.translatable("title.sanctumcraft.entered").formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
                Text.translatable("subtitle.sanctumcraft.time_left_difficulty",
                        Text.translatable(inst.difficulty.key()).formatted(inst.difficulty.color),
                        formatTime(remaining)).formatted(Formatting.GRAY),
                10, 50, 20);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.5F, 1.4F);
    }

    /** Exit portal inside the sanctum (or command): back to the return point. */
    public static void exitSanctum(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SanctumInstance inst = instanceOf(player);
        if (inst != null && player.isAlive()) {
            EggHandler.extract(player, inst);
        }
        ReturnPoint rp = inst != null ? inst.returns.get(player.getUuid()) : null;
        if (inst != null && inst.bossBar != null) inst.bossBar.removePlayer(player);
        if (rp != null) {
            RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(rp.dimension()));
            ServerWorld target = server.getWorld(key);
            if (target != null) {
                player.teleport(target, rp.x(), rp.y(), rp.z(), rp.yaw(), rp.pitch());
                PortalHandler.markContact(player);
                return;
            }
        }
        sendToSpawn(player);
    }

    private static void sendToSpawn(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerWorld overworld = server.getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        player.teleport(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0F, 0.0F);
        PortalHandler.markContact(player);
    }

    // ------------------------------------------------------------------------------------------
    // Creation / closing
    // ------------------------------------------------------------------------------------------

    /** Creates a sanctum in a free slot (generation starts on the next ticks). Null if no slot. */
    public static SanctumInstance create(MinecraftServer server, UUID portalId, SanctumDifficulty difficulty) {
        SanctumState state = state(server);
        ServerWorld world = sanctumWorld(server);
        if (world == null) return null;
        int slot = state.freeSlot(SanctumConfig.maxSimultaneousSanctums);
        if (slot < 0) return null;

        List<PlacedRoom> rooms = RoomLayout.generate(world.getRandom(), difficulty);
        BlockPos origin = new BlockPos(slot * SLOT_SPACING, BASE_Y, 0);
        SanctumInstance inst = new SanctumInstance(UUID.randomUUID(), portalId, slot, origin, rooms);
        inst.generationStartTick = now(server);
        inst.difficulty = difficulty;

        for (PlacedRoom r : rooms) {
            if (r.kind() == RoomKind.ENTRANCE) {
                // Fixed spawn spot of the entrance template, facing south (away from the exit portal).
                inst.spawn = inst.roomOrigin(r).add(12, 1, 9);
                inst.spawnYaw = 0.0F;
            }
        }
        state.instances.put(inst.id, inst);
        state.markDirty();
        SanctumCraft.LOGGER.info("Sanctum {} created in slot {} ({} rooms)", inst.id, slot, rooms.size());
        return inst;
    }

    /** Timer reached zero (or forced close): kill players inside, then start clearing. */
    public static void collapse(MinecraftServer server, SanctumInstance inst) {
        for (ServerPlayerEntity p : playersInside(server, inst)) {
            p.sendMessage(Text.translatable("message.sanctumcraft.collapsed").formatted(Formatting.DARK_RED));
            if (p.isSpectator()) {
                exitSanctum(p);
            } else {
                p.kill();
            }
        }
        startClosing(server, inst, true);
    }

    private static void startClosing(MinecraftServer server, SanctumInstance inst, boolean applyCooldown) {
        SanctumState state = state(server);
        inst.status = SanctumInstance.Status.CLOSING;
        inst.clearRoom = 0;
        inst.clearIndex = 0;
        inst.entitiesCleared = false;
        if (inst.bossBar != null) {
            inst.bossBar.clearPlayers();
            inst.bossBar = null;
        }
        for (UUID uuid : inst.waiting.keySet()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
            if (p != null) p.sendMessage(Text.translatable("message.sanctumcraft.generation_cancelled").formatted(Formatting.RED), true);
        }
        inst.waiting.clear();
        if (inst.portalId != null) {
            PortalRecord portal = state.portals.get(inst.portalId);
            if (portal != null) {
                if (inst.id.equals(portal.activeInstance)) portal.activeInstance = null;
                if (applyCooldown) portal.cooldownUntil = now(server) + SanctumConfig.portalCooldownSeconds * 20L;
            }
        }
        state.markDirty();
    }

    // ------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ServerWorld world = sanctumWorld(server);
        if (world == null) return;
        SanctumState state = state(server);
        if (state.instances.isEmpty()) return;

        List<UUID> finished = new ArrayList<>();
        for (SanctumInstance inst : new ArrayList<>(state.instances.values())) {
            switch (inst.status) {
                case GENERATING -> tickGenerating(server, world, inst);
                case ACTIVE -> tickActive(server, inst);
                case CLOSING -> {
                    if (tickClosing(world, inst)) finished.add(inst.id);
                }
            }
        }
        for (UUID id : finished) {
            state.instances.remove(id);
            SanctumCraft.LOGGER.info("Sanctum {} removed", id);
        }
        if (!finished.isEmpty()) state.markDirty();
    }

    private static void tickGenerating(MinecraftServer server, ServerWorld world, SanctumInstance inst) {
        for (int n = 0; n < SanctumConfig.roomsPlacedPerTick && inst.roomsPlaced < inst.rooms.size(); n++) {
            placeRoom(server, world, inst, inst.rooms.get(inst.roomsPlaced));
            inst.roomsPlaced++;
        }

        long elapsed = now(server) - inst.generationStartTick;
        // Keep waiting players frozen and informed.
        for (Iterator<Map.Entry<UUID, Vec3d>> it = inst.waiting.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Vec3d> e = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            Vec3d pos = e.getValue();
            if (p.getPos().squaredDistanceTo(pos) > 0.01) {
                p.requestTeleport(pos.x, pos.y, pos.z);
            }
            if (elapsed % 20 == 0) {
                title(p, Text.translatable("title.sanctumcraft.generating").formatted(Formatting.LIGHT_PURPLE),
                        Text.translatable("subtitle.sanctumcraft.generating", inst.roomsPlaced, inst.rooms.size())
                                .formatted(Formatting.GRAY), 0, 30, 5);
            }
        }

        if (inst.roomsPlaced >= inst.rooms.size() && elapsed >= SanctumConfig.minGenerationTicks) {
            inst.status = SanctumInstance.Status.ACTIVE;
            inst.endTick = now(server) + inst.difficulty.durationSeconds() * 20L;
            state(server).markDirty();
            for (UUID uuid : new ArrayList<>(inst.waiting.keySet())) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
                if (p != null) teleportIn(server, inst, p);
            }
            inst.waiting.clear();
        }
    }

    private static void placeRoom(MinecraftServer server, ServerWorld world, SanctumInstance inst, PlacedRoom room) {
        BlockPos base = inst.roomOrigin(room);
        if (room.kind() == RoomKind.CORRIDOR) {
            CorridorBuilder.build(world, base, room);
            return;
        }
        Identifier id = SanctumCraft.id(room.template());
        Optional<StructureTemplate> template = server.getStructureTemplateManager().getTemplate(id);
        if (template.isPresent()) {
            StructurePlacementData data = new StructurePlacementData().setIgnoreEntities(true).setUpdateNeighbors(false);
            template.get().place(world, base, base, data, world.getRandom(), Block.NOTIFY_LISTENERS);
        } else {
            SanctumCraft.LOGGER.error("Missing room template {}, placing a fallback floor", id);
            for (int x = 0; x < room.w() * RoomLayout.CELL; x++) {
                for (int z = 0; z < room.h() * RoomLayout.CELL; z++) {
                    world.setBlockState(base.add(x, 0, z), Blocks.DEEPSLATE_TILES.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
        sealDoors(world, base, room);

        if (room.kind() == RoomKind.NEST) {
            BlockPos nest = base.add(NEST_OFFSET);
            BlockState nestState = ModBlocks.EGG_NEST.getDefaultState().with(EggNestBlock.ACTIVE, room.nestActive());
            world.setBlockState(nest, nestState, Block.NOTIFY_LISTENERS);
        } else if (room.kind() == RoomKind.DESTINATION) {
            world.setBlockState(base.add(MECHANISM_OFFSET), ModBlocks.DESTINATION_MECHANISM.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    /** Fills every doorway of the room that does not lead to a neighbour. Doorway: 3 wide (11..13), 4 tall (y 1..4). */
    private static void sealDoors(ServerWorld world, BlockPos base, PlacedRoom room) {
        for (int lx = 0; lx < room.w(); lx++) {
            for (int lz = 0; lz < room.h(); lz++) {
                BlockPos cell = base.add(lx * RoomLayout.CELL, 0, lz * RoomLayout.CELL);
                int last = RoomLayout.CELL - 1;
                for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    boolean edge = switch (dir) {
                        case NORTH -> lz == 0;
                        case SOUTH -> lz == room.h() - 1;
                        case WEST -> lx == 0;
                        default -> lx == room.w() - 1;
                    };
                    if (!edge || room.hasDoor(lx, lz, dir)) continue;
                    for (int w = 11; w <= 13; w++) {
                        for (int y = 1; y <= 4; y++) {
                            BlockPos p = switch (dir) {
                                case NORTH -> cell.add(w, y, 0);
                                case SOUTH -> cell.add(w, y, last);
                                case WEST -> cell.add(0, y, w);
                                default -> cell.add(last, y, w);
                            };
                            world.setBlockState(p, SEAL, Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }
    }

    private static void tickActive(MinecraftServer server, SanctumInstance inst) {
        long remaining = inst.endTick - now(server);
        List<ServerPlayerEntity> inside = playersInside(server, inst);

        if (inst.bossBar == null) {
            inst.bossBar = new ServerBossBar(Text.empty(), BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
        }
        ServerBossBar bar = inst.bossBar;
        for (ServerPlayerEntity p : new ArrayList<>(bar.getPlayers())) {
            if (!inside.contains(p)) bar.removePlayer(p);
        }
        for (ServerPlayerEntity p : inside) {
            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        }
        if (now(server) % 20 == 0) {
            for (ServerPlayerEntity p : inside) EggHandler.applyWeight(p);
        }
        tickDestinationActivation(server, inst, inside);
        if (SanctumConfig.forceSurvival && !SanctumCraft.isDevEnvironment() && now(server) % 20 == 0) {
            for (ServerPlayerEntity p : inside) {
                if (p.interactionManager.getGameMode() != GameMode.SURVIVAL) p.changeGameMode(GameMode.SURVIVAL);
            }
        }
        bar.setName(Text.translatable("bossbar.sanctumcraft.timer",
                Text.translatable(inst.difficulty.key()).formatted(inst.difficulty.color), formatTime(remaining)));
        bar.setPercent(Math.max(0.0F, Math.min(1.0F, remaining / (inst.difficulty.durationSeconds() * 20.0F))));
        bar.setColor(remaining <= 60 * 20 ? BossBar.Color.RED : BossBar.Color.PURPLE);

        // Warnings at 5 min, 1 min and the last 10 seconds.
        long sec = Math.max(0, (remaining + 19) / 20);
        if (sec != inst.lastWarnSecond) {
            inst.lastWarnSecond = sec;
            if (sec == 300 || sec == 60) {
                for (ServerPlayerEntity p : inside) {
                    title(p, Text.empty(), Text.translatable("subtitle.sanctumcraft.warning", formatTime(remaining))
                            .formatted(Formatting.GOLD), 5, 40, 10);
                    p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.MASTER, 1.0F, 0.7F);
                }
            } else if (sec > 0 && sec <= 10) {
                for (ServerPlayerEntity p : inside) {
                    title(p, Text.literal(String.valueOf(sec)).formatted(Formatting.RED, Formatting.BOLD),
                            Text.translatable("subtitle.sanctumcraft.collapsing").formatted(Formatting.DARK_RED), 0, 20, 0);
                    p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.MASTER, 1.0F, 0.5F);
                }
            }
        }

        if (remaining <= 0) {
            collapse(server, inst);
        }
    }

    /** Clears a few thousand block positions per tick. Returns true when fully cleared. */
    private static boolean tickClosing(ServerWorld world, SanctumInstance inst) {
        if (!inst.entitiesCleared) {
            discardEntities(world, inst);
            inst.entitiesCleared = true;
        }
        int budget = SanctumConfig.blocksClearedPerTick;
        BlockState air = Blocks.AIR.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        while (budget > 0 && inst.clearRoom < inst.rooms.size()) {
            PlacedRoom room = inst.rooms.get(inst.clearRoom);
            BlockPos base = inst.roomOrigin(room);
            int sx = room.w() * RoomLayout.CELL;
            int sz = room.h() * RoomLayout.CELL;
            int layer = sx * sz;
            int volume = SanctumInstance.volume(room);
            while (budget > 0 && inst.clearIndex < volume) {
                int i = inst.clearIndex++;
                int y = i / layer;
                int rest = i % layer;
                m.set(base.getX() + rest % sx, base.getY() + y, base.getZ() + rest / sx);
                if (!world.getBlockState(m).isAir()) {
                    world.setBlockState(m, air, CLEAR_FLAGS);
                }
                budget--;
            }
            if (inst.clearIndex >= volume) {
                inst.clearRoom++;
                inst.clearIndex = 0;
            }
        }
        if (inst.clearRoom >= inst.rooms.size()) {
            // Containers emptied by the clearing may have spilled items: sweep again.
            discardEntities(world, inst);
            return true;
        }
        return false;
    }

    private static void discardEntities(ServerWorld world, SanctumInstance inst) {
        BlockBox b = inst.bounds();
        Box box = new Box(b.getMinX(), b.getMinY(), b.getMinZ(), b.getMaxX() + 1, b.getMaxY() + 1, b.getMaxZ() + 1);
        for (Entity e : world.getEntitiesByClass(Entity.class, box, e -> !(e instanceof PlayerEntity))) {
            e.discard();
        }
    }

    // ------------------------------------------------------------------------------------------
    // Destination mechanism
    // ------------------------------------------------------------------------------------------

    public static void startDestinationActivation(ServerPlayerEntity player, BlockPos pos) {
        MinecraftServer server = player.getServer();
        if (server == null || !(player.getWorld() instanceof ServerWorld world)) return;
        SanctumInstance inst = instanceAt(world, pos);
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) return;
        if (inst.activatingPlayer != null) {
            if (!inst.activatingPlayer.equals(player.getUuid())) {
                player.sendMessage(Text.translatable("message.sanctumcraft.destination.busy").formatted(Formatting.GRAY), true);
            }
            return;
        }
        inst.activatingPlayer = player.getUuid();
        inst.activationStart = now(server);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0F, 1.2F);
    }

    private static void tickDestinationActivation(MinecraftServer server, SanctumInstance inst, List<ServerPlayerEntity> inside) {
        if (inst.activatingPlayer == null) return;
        PlacedRoom room = inst.destinationRoom();
        ServerWorld world = sanctumWorld(server);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(inst.activatingPlayer);
        if (room == null || world == null) {
            inst.activatingPlayer = null;
            return;
        }
        BlockPos mechanism = inst.roomOrigin(room).add(MECHANISM_OFFSET);
        if (player == null || !player.isAlive() || !inside.contains(player)
                || player.getPos().squaredDistanceTo(mechanism.toCenterPos()) > 16) {
            if (player != null) {
                player.sendMessage(Text.translatable("message.sanctumcraft.destination.cancelled").formatted(Formatting.RED), true);
            }
            inst.activatingPlayer = null;
            return;
        }
        long needed = Math.max(1, SanctumConfig.destinationActivationSeconds * 20L);
        long elapsed = now(server) - inst.activationStart;
        if (elapsed < needed) {
            if (elapsed % 5 == 0) {
                int filled = (int) (10 * elapsed / needed);
                player.sendMessage(Text.translatable("message.sanctumcraft.destination.activating",
                        "■".repeat(filled) + "□".repeat(10 - filled)).formatted(Formatting.AQUA), true);
                world.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD,
                        mechanism.getX() + 0.5, mechanism.getY() + 1.2, mechanism.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
            }
            return;
        }
        inst.activatingPlayer = null;
        openDestinationPortal(world, inst, room);
        for (ServerPlayerEntity p : inside) {
            title(p, Text.empty(), Text.translatable("subtitle.sanctumcraft.destination.open").formatted(Formatting.AQUA), 5, 50, 15);
        }
    }

    private static void openDestinationPortal(ServerWorld world, SanctumInstance inst, PlacedRoom room) {
        BlockPos base = inst.roomOrigin(room);
        BlockPos mechanism = base.add(MECHANISM_OFFSET);
        BlockState ms = world.getBlockState(mechanism);
        if (ms.isOf(ModBlocks.DESTINATION_MECHANISM)) {
            world.setBlockState(mechanism, ms.with(DestinationMechanismBlock.ACTIVE, true), Block.NOTIFY_LISTENERS);
        }
        BlockState portal = ModBlocks.SANCTUM_PORTAL.getDefaultState().with(SanctumPortalBlock.AXIS, Direction.Axis.X);
        BlockPos frame = base.add(EXIT_FRAME_OFFSET);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                world.setBlockState(frame.add(x, y, 0), portal, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
        world.playSound(null, frame, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------------------------------
    // Player-placed blocks
    // ------------------------------------------------------------------------------------------

    public static void markPlaced(ServerWorld world, BlockPos pos) {
        SanctumInstance inst = state(world.getServer()).findInstanceAt(pos);
        if (inst != null && inst.playerPlaced.add(pos.asLong())) state(world.getServer()).markDirty();
    }

    public static void unmarkPlaced(ServerWorld world, BlockPos pos) {
        SanctumInstance inst = state(world.getServer()).findInstanceAt(pos);
        if (inst != null && inst.playerPlaced.remove(pos.asLong())) state(world.getServer()).markDirty();
    }

    public static boolean isPlayerPlaced(ServerWorld world, BlockPos pos) {
        SanctumInstance inst = state(world.getServer()).findInstanceAt(pos);
        return inst != null && inst.playerPlaced.contains(pos.asLong());
    }

    /** True if a game mode change must be refused for this player (inside a sanctum, production). */
    public static boolean blocksGameModeChange(ServerPlayerEntity player, GameMode target) {
        return SanctumConfig.forceSurvival
                && target != GameMode.SURVIVAL
                && !SanctumCraft.isDevEnvironment()
                && ModDimensions.isSanctum(player.getWorld());
    }

    // ------------------------------------------------------------------------------------------
    // Debug / admin
    // ------------------------------------------------------------------------------------------

    /** Opens a sanctum without portal; the player is sent back to their current position on exit. */
    public static boolean debugGenerate(ServerPlayerEntity player, SanctumDifficulty difficulty) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        SanctumInstance inst = create(server, null, difficulty);
        if (inst == null) return false;
        join(server, inst, player);
        return true;
    }

    public static Collection<SanctumInstance> instances(MinecraftServer server) {
        return state(server).instances.values();
    }

    public static Collection<PortalRecord> portals(MinecraftServer server) {
        return state(server).portals.values();
    }

    public static void setRemaining(MinecraftServer server, SanctumInstance inst, int seconds) {
        inst.endTick = now(server) + seconds * 20L;
        inst.lastWarnSecond = -1;
        state(server).markDirty();
    }

    public static long remainingTicks(MinecraftServer server, SanctumInstance inst) {
        return inst.endTick - now(server);
    }

    public static long cooldownTicks(MinecraftServer server, PortalRecord portal) {
        return Math.max(0, portal.cooldownUntil - now(server));
    }

    public static int resetCooldowns(MinecraftServer server) {
        int n = 0;
        for (PortalRecord r : state(server).portals.values()) {
            if (r.cooldownUntil > 0) n++;
            r.cooldownUntil = 0;
        }
        state(server).markDirty();
        return n;
    }

    public static PortalRecord nearestPortal(ServerPlayerEntity player, double maxDistance) {
        if (player.getServer() == null) return null;
        String dim = dimId(player.getWorld());
        PortalRecord best = null;
        double bestDist = maxDistance * maxDistance;
        for (PortalRecord r : state(player.getServer()).portals.values()) {
            if (!r.dimension.equals(dim)) continue;
            double d = r.box.getCenter().getSquaredDistance(player.getPos());
            if (d <= bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }
}

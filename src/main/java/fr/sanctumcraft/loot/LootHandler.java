package fr.sanctumcraft.loot;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.PlacedRoom;
import fr.sanctumcraft.instance.RoomKind;
import fr.sanctumcraft.instance.RoomLayout;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Chest spots, loot rolling, progressive reveal and vault doors. */
public final class LootHandler {

    /** Percent of loot spots that stay empty. */
    private static final int EMPTY_SPOT_CHANCE = 30;
    private static final int FIRST_REVEAL_DELAY = 10;

    private LootHandler() {}

    // ------------------------------------------------------------------ generation

    /**
     * Replaces the loot spot markers of a freshly placed room with chests (or nothing).
     * Vaults get richer chests and never empty spots.
     */
    public static void placeChests(ServerWorld world, SanctumInstance inst, PlacedRoom room, BlockPos base) {
        Random random = world.getRandom();
        int sx = room.w() * RoomLayout.CELL;
        int sz = room.h() * RoomLayout.CELL;
        BlockPos.Mutable m = new BlockPos.Mutable();
        double cx = base.getX() + sx / 2.0;
        double cz = base.getZ() + sz / 2.0;
        for (int y = 0; y < RoomLayout.ROOM_HEIGHT; y++) {
            for (int x = 0; x < sx; x++) {
                for (int z = 0; z < sz; z++) {
                    m.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    if (!world.getBlockState(m).isOf(ModBlocks.LOOT_SPOT)) continue;
                    boolean vault = room.kind() == RoomKind.VAULT;
                    if (!vault && random.nextInt(100) < EMPTY_SPOT_CHANCE) {
                        world.setBlockState(m, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        continue;
                    }
                    ChestTier tier = ChestTier.roll(random, inst.difficulty);
                    if (vault) tier = tier.bump(2);
                    else if (room.kind() == RoomKind.GREAT_HALL) tier = tier.bump(random.nextInt(2));
                    Direction facing = Direction.getFacing(cx - (m.getX() + 0.5), 0, cz - (m.getZ() + 0.5));
                    BlockState chest = ModBlocks.chestFor(tier).getDefaultState()
                            .with(SanctumChestBlock.FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
                    world.setBlockState(m, chest, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    /** Fills the single doorway of a vault room with locked doors. */
    public static void lockVault(ServerWorld world, BlockPos base, PlacedRoom room, KeyTier tier) {
        int last = RoomLayout.CELL - 1;
        BlockState door = ModBlocks.vaultDoorFor(tier).getDefaultState();
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (!room.hasDoor(0, 0, dir)) continue;
            for (int w = 11; w <= 13; w++) {
                for (int y = 1; y <= 4; y++) {
                    BlockPos p = switch (dir) {
                        case NORTH -> base.add(w, y, 0);
                        case SOUTH -> base.add(w, y, last);
                        case WEST -> base.add(0, y, w);
                        default -> base.add(last, y, w);
                    };
                    world.setBlockState(p, door, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    // ------------------------------------------------------------------ chests

    public static void openChest(ServerPlayerEntity player, BlockPos pos, SanctumChestBlock block) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;
        SanctumInstance inst = InstanceManager.instanceAt(world, pos);
        if (inst == null) {
            player.sendMessage(Text.translatable("message.sanctumcraft.chest.inert").formatted(Formatting.GRAY), true);
            return;
        }
        LootChest chest = inst.chests.get(pos.asLong());
        if (chest == null) {
            chest = createChest(world, player, pos, block.tier, inst);
            inst.chests.put(pos.asLong(), chest);
            BlockState state = world.getBlockState(pos);
            world.setBlockState(pos, state.with(SanctumChestBlock.OPENED, true), Block.NOTIFY_LISTENERS);
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.8F, 0.9F);
        }
        LootChest c = chest;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new LootChestMenu(syncId, inv, c, player, pos),
                block.getName()));
    }

    private static LootChest createChest(ServerWorld world, ServerPlayerEntity player, BlockPos pos, ChestTier tier,
                                         SanctumInstance inst) {
        LootChest chest = new LootChest(tier);
        List<ItemStack> items = new ArrayList<>();
        RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, SanctumCraft.id("chests/sanctum_" + tier.id()));
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        LootContextParameterSet params = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, pos.toCenterPos())
                .add(LootContextParameters.THIS_ENTITY, player)
                .luck(player.getLuck())
                .build(LootContextTypes.CHEST);

        // Difficulty multiplier: 1.75 = one roll + 75 % chance of a second one, etc.
        float mult = inst.difficulty.loot;
        int rolls = (int) mult + (world.getRandom().nextFloat() < (mult - (int) mult) ? 1 : 0);
        for (int i = 0; i < Math.max(1, rolls); i++) {
            table.generateLoot(params, items::add);
        }
        if (world.getRandom().nextInt(100) < tier.keyChance) {
            items.add(new ItemStack(ModItems.keyFor(KeyTier.forDifficulty(inst.difficulty))));
        }
        while (items.size() > 27) items.remove(items.size() - 1);

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, new java.util.Random(world.getRandom().nextLong()));
        for (int i = 0; i < items.size(); i++) {
            chest.pending.add(items.get(i));
            chest.pendingSlots.add(slots.get(i));
            chest.inventory.setStack(slots.get(i), LootChest.placeholder());
        }
        chest.total = items.size();
        chest.timer = FIRST_REVEAL_DELAY;
        return chest;
    }

    /** Called every tick for every open chest screen; advances the reveal at most once per tick. */
    static void tickReveal(ServerWorld world, LootChest chest, BlockPos pos, ServerPlayerEntity viewer) {
        if (!chest.isRevealing()) return;
        long now = world.getTime();
        if (chest.lastTick != now) {
            chest.lastTick = now;
            if (--chest.timer <= 0) {
                ItemStack item = chest.pending.remove(0);
                int slot = chest.pendingSlots.remove(0);
                chest.inventory.setStack(slot, item);
                float pitch = switch (item.getRarity()) {
                    case COMMON -> 1.0F;
                    case UNCOMMON -> 1.2F;
                    case RARE -> 1.4F;
                    case EPIC -> 1.7F;
                };
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.6F, pitch);
                chest.timer = chest.pending.isEmpty() ? 0 : LootChest.revealTicks(chest.pending.get(0));
            }
        }
        int filled = chest.total == 0 ? 10 : Math.round(10f * chest.revealed() / chest.total);
        viewer.sendMessage(Text.translatable("message.sanctumcraft.chest.searching",
                "■".repeat(filled) + "□".repeat(10 - filled)).formatted(Formatting.BLUE), true);
    }

    // ------------------------------------------------------------------ vaults

    static void openVault(ServerPlayerEntity player, BlockPos pos, VaultDoorBlock block, ItemStack key) {
        ServerWorld world = (ServerWorld) player.getWorld();
        // Open every connected door block of the same doorway.
        List<BlockPos> todo = new ArrayList<>();
        List<BlockPos> done = new ArrayList<>();
        todo.add(pos);
        while (!todo.isEmpty() && done.size() < 64) {
            BlockPos p = todo.remove(todo.size() - 1);
            if (done.contains(p) || !world.getBlockState(p).isOf(block)) continue;
            done.add(p);
            for (Direction d : Direction.values()) todo.add(p.offset(d));
        }
        for (BlockPos p : done) world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        if (!player.getAbilities().creativeMode) key.decrement(1);
        world.playSound(null, pos, SoundEvents.BLOCK_VAULT_OPEN_SHUTTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("message.sanctumcraft.vault.opened").formatted(Formatting.GOLD), true);
    }
}

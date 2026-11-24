package fr.sanctumcraft.mob;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.instance.PlacedRoom;
import fr.sanctumcraft.instance.RoomKind;
import fr.sanctumcraft.instance.RoomLayout;
import fr.sanctumcraft.instance.SanctumDifficulty;
import fr.sanctumcraft.instance.SanctumInstance;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Sanctum monsters: vanilla mobs, persistent, tagged, scaled by difficulty and by the
 * reinforcement waves. They never leave the sanctum and vanish with it.
 */
public final class SanctumMobs {

    public static final String TAG = "sanctumcraft_mob";
    private static final Identifier HEALTH_ID = SanctumCraft.id("difficulty_health");
    private static final Identifier DAMAGE_ID = SanctumCraft.id("difficulty_damage");
    private static final Identifier WAVE_ID = SanctumCraft.id("wave_damage");

    private SanctumMobs() {}

    // ------------------------------------------------------------------ mob pools

    private record Weighted(EntityType<? extends MobEntity> type, int weight) {}

    private static List<Weighted> pool(SanctumDifficulty d) {
        List<Weighted> l = new ArrayList<>();
        l.add(new Weighted(EntityType.ZOMBIE, 4));
        l.add(new Weighted(EntityType.SKELETON, 3));
        l.add(new Weighted(EntityType.SPIDER, 2));
        l.add(new Weighted(EntityType.HUSK, 1));
        l.add(new Weighted(EntityType.CAVE_SPIDER, 1));
        if (d != SanctumDifficulty.NORMAL) {
            l.add(new Weighted(EntityType.STRAY, 2));
            l.add(new Weighted(EntityType.WITCH, 1));
            l.add(new Weighted(EntityType.VINDICATOR, 1));
        }
        if (d == SanctumDifficulty.CHAOS) {
            l.add(new Weighted(EntityType.WITHER_SKELETON, 3));
            l.add(new Weighted(EntityType.VINDICATOR, 2));
            l.add(new Weighted(EntityType.EVOKER, 1));
        }
        return l;
    }

    private static EntityType<? extends MobEntity> pick(Random random, SanctumDifficulty d) {
        List<Weighted> l = pool(d);
        int total = 0;
        for (Weighted w : l) total += w.weight();
        int r = random.nextInt(total);
        for (Weighted w : l) {
            if (r < w.weight()) return w.type();
            r -= w.weight();
        }
        return EntityType.ZOMBIE;
    }

    // ------------------------------------------------------------------ spawning

    /** Spawns one sanctum mob near {@code around} (searching a free spot within {@code radius}). Null if no spot. */
    public static MobEntity spawn(ServerWorld world, SanctumInstance inst, BlockPos around, int radius) {
        BlockPos pos = findSpot(world, around, radius, world.getRandom());
        if (pos == null) return null;
        MobEntity mob = pick(world.getRandom(), inst.difficulty).create(world);
        if (mob == null) return null;
        mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.getRandom().nextFloat() * 360F, 0F);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
        mob.setPersistent();
        mob.addCommandTag(TAG);
        mob.setCustomName(Text.translatable("entity.sanctumcraft.guardian", mob.getType().getName())
                .formatted(inst.difficulty.color));
        applyDifficulty(world, mob, inst);
        world.spawnEntity(mob);
        return mob;
    }

    /** Free spot: solid floor, two air blocks above. */
    private static BlockPos findSpot(ServerWorld world, BlockPos around, int radius, Random random) {
        for (int i = 0; i < 16; i++) {
            BlockPos p = i == 0 ? around
                    : around.add(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
            for (int dy = 0; dy >= -2; dy--) {
                BlockPos feet = p.up(dy);
                BlockState floor = world.getBlockState(feet.down());
                if (floor.isSideSolidFullSquare(world, feet.down(), Direction.UP)
                        && world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                        && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()) {
                    return feet;
                }
            }
        }
        return null;
    }

    /** Health and damage from the difficulty, plus the current wave bonus. */
    private static void applyDifficulty(ServerWorld world, MobEntity mob, SanctumInstance inst) {
        SanctumDifficulty d = inst.difficulty;
        EntityAttributeInstance health = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null && d.mobHealth != 1.0F) {
            health.overwritePersistentModifier(new EntityAttributeModifier(HEALTH_ID, d.mobHealth - 1.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        EntityAttributeInstance damage = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null && d.mobDamage != 1.0F) {
            damage.overwritePersistentModifier(new EntityAttributeModifier(DAMAGE_ID, d.mobDamage - 1.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        applyWave(world, mob, inst.damageLevel, d);
    }

    /** Wave bonus: +25 % / +50 % melee damage, and Power on bows. */
    private static void applyWave(ServerWorld world, MobEntity mob, int level, SanctumDifficulty d) {
        EntityAttributeInstance damage = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null && level > 0) {
            damage.overwritePersistentModifier(new EntityAttributeModifier(WAVE_ID, 0.25 * level,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        ItemStack hand = mob.getMainHandStack();
        if (hand.getItem() instanceof BowItem) {
            int power = (d == SanctumDifficulty.NORMAL ? 0 : d == SanctumDifficulty.NIGHTMARE ? 1 : 2) + level;
            if (power > 0) {
                RegistryEntry<Enchantment> entry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                        .entryOf(Enchantments.POWER);
                hand.addEnchantment(entry, power);
            }
        }
    }

    /** Applies a new wave level to every living mob of the sanctum. */
    public static void upgradeAll(ServerWorld world, SanctumInstance inst) {
        for (MobEntity mob : mobsOf(world, inst)) applyWave(world, mob, inst.damageLevel, inst.difficulty);
    }

    public static List<MobEntity> mobsOf(ServerWorld world, SanctumInstance inst) {
        BlockBox b = inst.bounds();
        Box box = new Box(b.getMinX(), b.getMinY(), b.getMinZ(), b.getMaxX() + 1, b.getMaxY() + 1, b.getMaxZ() + 1);
        return world.getEntitiesByClass(MobEntity.class, box, m -> m.isAlive() && m.getCommandTags().contains(TAG));
    }

    // ------------------------------------------------------------------ population

    private static int packSize(Random random, SanctumDifficulty d) {
        return switch (d) {
            case NORMAL -> 1 + random.nextInt(2);
            case NIGHTMARE -> 2 + random.nextInt(2);
            case CHAOS -> 2 + random.nextInt(3);
        };
    }

    private static BlockPos randomPointIn(SanctumInstance inst, PlacedRoom room, Random random) {
        BlockPos base = inst.roomOrigin(room);
        int sx = room.w() * RoomLayout.CELL, sz = room.h() * RoomLayout.CELL;
        if (room.kind() == RoomKind.CORRIDOR || room.kind() == RoomKind.SMALL || room.kind() == RoomKind.VAULT) {
            return base.add(12, 1, 12);
        }
        return base.add(4 + random.nextInt(sx - 8), 1, 4 + random.nextInt(sz - 8));
    }

    private static void spawnPack(ServerWorld world, SanctumInstance inst, BlockPos around, int count) {
        int cap = SanctumConfig.maxMobsPerSanctum;
        int alive = mobsOf(world, inst).size();
        for (int i = 0; i < count && alive < cap; i++) {
            if (spawn(world, inst, around, 4) != null) alive++;
        }
    }

    /** Initial population + fixed guards. Called once, when the sanctum opens. */
    public static void populate(ServerWorld world, SanctumInstance inst) {
        Random random = world.getRandom();
        int density = SanctumConfig.mobDensityPercent;
        for (PlacedRoom room : inst.rooms) {
            BlockPos base = inst.roomOrigin(room);
            switch (room.kind()) {
                case ENTRANCE -> { }
                case NEST -> spawnPack(world, inst, base.add(12, 1, 7), scaled(guardCount(inst), density));
                case DESTINATION -> spawnPack(world, inst, base.add(12, 1, 7), scaled(guardCount(inst) + 1, density));
                case VAULT -> {
                    spawnPack(world, inst, vaultFront(base, room), scaled(guardCount(inst), density));
                    spawnPack(world, inst, base.add(12, 1, 12), scaled(1, density));
                }
                case GREAT_HALL -> {
                    for (int i = 0; i < 2; i++) {
                        if (random.nextInt(100) < 70) {
                            spawnPack(world, inst, randomPointIn(inst, room, random), scaled(packSize(random, inst.difficulty), density));
                        }
                    }
                }
                case CORRIDOR -> {
                    if (random.nextInt(100) < 25) spawnPack(world, inst, base.add(12, 1, 12), scaled(1, density));
                }
                default -> {
                    if (random.nextInt(100) < 60) {
                        spawnPack(world, inst, randomPointIn(inst, room, random), scaled(packSize(random, inst.difficulty), density));
                    }
                }
            }
        }
    }

    /** Reinforcements: a number of packs dropped in random non-entrance rooms. */
    public static void reinforce(ServerWorld world, SanctumInstance inst, int packs) {
        Random random = world.getRandom();
        List<PlacedRoom> rooms = new ArrayList<>();
        for (PlacedRoom r : inst.rooms) if (r.kind() != RoomKind.ENTRANCE) rooms.add(r);
        if (rooms.isEmpty()) return;
        for (int i = 0; i < scaled(packs, SanctumConfig.mobDensityPercent); i++) {
            PlacedRoom room = rooms.get(random.nextInt(rooms.size()));
            spawnPack(world, inst, randomPointIn(inst, room, random), packSize(random, inst.difficulty));
        }
    }

    private static int guardCount(SanctumInstance inst) {
        return inst.difficulty == SanctumDifficulty.NORMAL ? 2 : 3;
    }

    private static int scaled(int n, int percent) {
        return Math.max(n > 0 ? 1 : 0, Math.round(n * percent / 100f));
    }

    /** Just outside the single door of a vault room. */
    private static BlockPos vaultFront(BlockPos base, PlacedRoom room) {
        int c = RoomLayout.CELL;
        if (room.hasDoor(0, 0, Direction.NORTH)) return base.add(12, 1, -4);
        if (room.hasDoor(0, 0, Direction.SOUTH)) return base.add(12, 1, c + 3);
        if (room.hasDoor(0, 0, Direction.WEST)) return base.add(-4, 1, 12);
        return base.add(c + 3, 1, 12);
    }

    public static boolean isSanctumMob(Entity e) {
        return e.getCommandTags().contains(TAG);
    }
}

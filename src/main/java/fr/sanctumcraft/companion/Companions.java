package fr.sanctumcraft.companion;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.block.EggNestBlock;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggRarity;
import fr.sanctumcraft.instance.PlacedRoom;
import fr.sanctumcraft.instance.RoomKind;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.registry.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.WolfVariant;
import net.minecraft.entity.passive.WolfVariants;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TeleportTarget;

import java.util.ArrayList;
import java.util.List;

/** Companions hatched from Sanctum eggs: tamed wolves or cats with a rarity and traits. */
public final class Companions {

    public static final String TAG = "sanctumcraft_companion";
    private static final Identifier HEALTH_ID = SanctumCraft.id("companion_health");
    private static final Identifier DAMAGE_ID = SanctumCraft.id("companion_damage");
    private static final Identifier SPEED_ID = SanctumCraft.id("companion_speed");

    private Companions() {}

    private static double statMultiplier(EggRarity r) {
        return switch (r) {
            case COMMON -> 1.0;
            case RARE -> 1.3;
            case EPIC -> 1.7;
            case LEGENDARY -> 2.2;
        };
    }

    private static int traitCount(EggRarity r) {
        return switch (r) {
            case COMMON, RARE -> 1;
            case EPIC -> 2;
            case LEGENDARY -> 3;
        };
    }

    private static RegistryKey<WolfVariant> variant(EggRarity r, Random random) {
        List<RegistryKey<WolfVariant>> l = switch (r) {
            case COMMON -> List.of(WolfVariants.PALE, WolfVariants.WOODS, WolfVariants.ASHEN);
            case RARE -> List.of(WolfVariants.CHESTNUT, WolfVariants.RUSTY, WolfVariants.SPOTTED);
            case EPIC -> List.of(WolfVariants.STRIPED, WolfVariants.BLACK);
            case LEGENDARY -> List.of(WolfVariants.SNOWY);
        };
        return l.get(random.nextInt(l.size()));
    }

    public static void hatch(ServerPlayerEntity owner, ServerWorld world, BlockPos pos, EggRarity rarity) {
        Random random = world.getRandom();
        boolean cat = random.nextInt(100) < 25;
        TameableEntity pet = cat ? EntityType.CAT.create(world) : EntityType.WOLF.create(world);
        if (pet == null) return;
        pet.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, owner.getYaw() + 180F, 0F);
        pet.setTamed(true, true);
        pet.setOwner(owner);
        pet.setPersistent();
        pet.addCommandTag(TAG);
        pet.addCommandTag("sc_rarity_" + rarity.name().toLowerCase());
        if (pet instanceof WolfEntity wolf) {
            world.getRegistryManager().get(RegistryKeys.WOLF_VARIANT).getEntry(variant(rarity, random))
                    .ifPresent(wolf::setVariant);
        }

        // Traits
        List<CompanionTrait> pool = new ArrayList<>(cat
                ? List.of(CompanionTrait.HEALER, CompanionTrait.TRACKER, CompanionTrait.GUARDIAN, CompanionTrait.SWIFT)
                : List.of(CompanionTrait.FEROCIOUS, CompanionTrait.STURDY, CompanionTrait.SWIFT,
                CompanionTrait.HEALER, CompanionTrait.GUARDIAN, CompanionTrait.TRACKER));
        List<CompanionTrait> traits = new ArrayList<>();
        for (int i = 0; i < traitCount(rarity) && !pool.isEmpty(); i++) {
            traits.add(pool.remove(random.nextInt(pool.size())));
        }
        for (CompanionTrait t : traits) pet.addCommandTag(t.tag());

        // Stats
        double mult = statMultiplier(rarity);
        double health = mult * (traits.contains(CompanionTrait.STURDY) ? 1.5 : 1.0) - 1.0;
        double damage = mult * (traits.contains(CompanionTrait.FEROCIOUS) ? 1.25 : 1.0) - 1.0;
        modifier(pet.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH), HEALTH_ID, health);
        modifier(pet.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DAMAGE_ID, damage);
        if (traits.contains(CompanionTrait.SWIFT)) {
            modifier(pet.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), SPEED_ID, 0.2);
        }
        pet.setHealth(pet.getMaxHealth());

        MutableText name = Text.translatable("entity.sanctumcraft.companion",
                Text.translatable(rarity.key()), pet.getType().getName()).formatted(rarity.color);
        pet.setCustomName(name);
        world.spawnEntity(pet);

        world.spawnParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.3, 0.4, 0);
        world.playSound(null, pos, SoundEvents.BLOCK_SNIFFER_EGG_HATCH, SoundCategory.BLOCKS, 1.0F, 1.0F);
        MutableText traitText = Text.empty();
        for (int i = 0; i < traits.size(); i++) {
            if (i > 0) traitText.append(", ");
            traitText.append(Text.translatable(traits.get(i).key()));
        }
        owner.sendMessage(Text.translatable("message.sanctumcraft.companion.hatched", name, traitText)
                .formatted(Formatting.GREEN));
    }

    private static void modifier(EntityAttributeInstance inst, Identifier id, double value) {
        if (inst == null || value == 0) return;
        inst.overwritePersistentModifier(new EntityAttributeModifier(id, value,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ------------------------------------------------------------------ queries

    public static boolean has(Entity e, CompanionTrait t) {
        return e.getCommandTags().contains(t.tag());
    }

    /** Companions of {@code owner} within {@code radius} blocks, alive, not sitting. */
    public static List<TameableEntity> near(ServerPlayerEntity owner, double radius) {
        Box box = owner.getBoundingBox().expand(radius);
        return owner.getServerWorld().getEntitiesByClass(TameableEntity.class, box, e -> e.isAlive()
                && e.getCommandTags().contains(TAG) && owner.getUuid().equals(e.getOwnerUuid()) && !e.isSitting());
    }

    /** Brings up to N companions (close to the player) along to {@code world} at {@code pos}. */
    public static void bringAlong(List<TameableEntity> pets, ServerWorld world, Vec3d pos) {
        int n = 0;
        for (TameableEntity pet : pets) {
            if (n++ >= SanctumConfig.maxCompanionsInSanctum) break;
            pet.teleportTo(new TeleportTarget(world, pos, Vec3d.ZERO, pet.getYaw(), pet.getPitch(), TeleportTarget.NO_OP));
        }
    }

    // ------------------------------------------------------------------ dynamic traits (every second, in a sanctum)

    public static void tickInSanctum(ServerPlayerEntity owner, SanctumInstance inst, int serverTicks) {
        List<TameableEntity> pets = near(owner, 12);
        if (pets.isEmpty()) return;
        boolean healer = false, tracker = false, guardian = false;
        for (TameableEntity p : pets) {
            healer |= has(p, CompanionTrait.HEALER);
            tracker |= has(p, CompanionTrait.TRACKER);
            guardian |= has(p, CompanionTrait.GUARDIAN);
        }
        if (guardian) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
        }
        if (healer && serverTicks % 200 == 0 && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal(2.0F);
            owner.getServerWorld().spawnParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2, owner.getZ(), 2, 0.3, 0.1, 0.3, 0);
        }
        if (tracker && serverTicks % 400 == 0) {
            pointToNest(owner, inst, Text.translatable("message.sanctumcraft.companion.tracker"));
        }
    }

    /** Action bar "nearest active nest: direction + distance" (also used by the Night Eyes talent). */
    public static void pointToNest(ServerPlayerEntity player, SanctumInstance inst, Text prefix) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlacedRoom r : inst.rooms) {
            if (r.kind() != RoomKind.NEST) continue;
            BlockPos nest = inst.roomOrigin(r).add(12, 3, 12);
            var st = player.getServerWorld().getBlockState(nest);
            if (!st.isOf(ModBlocks.EGG_NEST) || !st.get(EggNestBlock.ACTIVE)) continue;
            double d = nest.getSquaredDistance(player.getPos());
            if (d < bestDist) {
                bestDist = d;
                best = nest;
            }
        }
        if (best == null) {
            player.sendMessage(prefix.copy().append(Text.translatable("message.sanctumcraft.nest.none")).formatted(Formatting.GRAY), true);
            return;
        }
        double dx = best.getX() + 0.5 - player.getX();
        double dz = best.getZ() + 0.5 - player.getZ();
        String[] dirs = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int idx = (int) Math.floorMod(Math.round(angle / 45.0), 8);
        player.sendMessage(prefix.copy().append(Text.translatable("message.sanctumcraft.nest.direction",
                Text.translatable("direction.sanctumcraft." + dirs[idx]), (int) Math.sqrt(bestDist)))
                .formatted(Formatting.GREEN), true);
    }
}

package fr.sanctumcraft.companion;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.block.EggNestBlock;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.egg.EggRarity;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.PlacedRoom;
import fr.sanctumcraft.instance.RoomKind;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.mixin.MobEntityAccessor;
import fr.sanctumcraft.profile.HunterProfile;
import fr.sanctumcraft.profile.ProfileState;
import fr.sanctumcraft.profile.Profiles;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.WolfVariant;
import net.minecraft.entity.passive.WolfVariants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 * Companion collection (Aniimo style): eggs hatch into the owner's collection, one companion is
 * active and summoned with the whistle. A defeated companion is only knocked out (cooldown).
 */
public final class Companions {

    /** Tag of summoned companion entities (0.6.0 wolves / cats use another tag and stay normal pets). */
    public static final String TAG = "sanctumcraft_pet";
    private static final Identifier HEALTH_ID = SanctumCraft.id("companion_health");
    private static final Identifier DAMAGE_ID = SanctumCraft.id("companion_damage");
    private static final Identifier SPEED_ID = SanctumCraft.id("companion_speed");

    /** Owner -> summoned entity uuid. */
    private static final Map<UUID, UUID> SUMMONED = new HashMap<>();
    /** Entity uuid -> owner. */
    private static final Map<UUID, UUID> OWNER_OF = new HashMap<>();
    /** Release confirmation: player -> companion id, until tick. */
    private static final Map<UUID, UUID> PENDING_RELEASE = new HashMap<>();

    private Companions() {}

    // ------------------------------------------------------------------ hatching

    private static double[] statRange(EggRarity r) {
        return switch (r) {
            case COMMON -> new double[]{-0.20, 0.20};
            case RARE -> new double[]{-0.10, 0.30};
            case EPIC -> new double[]{0.00, 0.45};
            case LEGENDARY -> new double[]{0.10, 0.60};
        };
    }

    private static int traitCount(EggRarity r) {
        return switch (r) {
            case COMMON, RARE -> 1;
            case EPIC -> 2;
            case LEGENDARY -> 3;
        };
    }

    private static CompanionSpecies rollSpecies(Random random, EggRarity r) {
        boolean golem = r == EggRarity.EPIC || r == EggRarity.LEGENDARY;
        int roll = random.nextInt(100);
        if (golem) {
            if (roll < 35) return CompanionSpecies.WOLF;
            if (roll < 60) return CompanionSpecies.FOX;
            if (roll < 80) return CompanionSpecies.ALLAY;
            return CompanionSpecies.IRON_GOLEM;
        }
        if (roll < 45) return CompanionSpecies.WOLF;
        if (roll < 75) return CompanionSpecies.FOX;
        return CompanionSpecies.ALLAY;
    }

    /** Creates a random companion of the given rarity. */
    public static CompanionData roll(Random random, EggRarity rarity) {
        CompanionSpecies species = rollSpecies(random, rarity);
        double[] range = statRange(rarity);
        double h = 1 + range[0] + random.nextDouble() * (range[1] - range[0]);
        double d = 1 + range[0] + random.nextDouble() * (range[1] - range[0]);
        double s = 1 + range[0] / 2 + random.nextDouble() * (range[1] - range[0]) / 2;
        List<CompanionTrait> pool = new ArrayList<>(species.traits);
        Map<CompanionTrait, Double> traits = new EnumMap<>(CompanionTrait.class);
        for (int i = 0; i < traitCount(rarity) && !pool.isEmpty(); i++) {
            traits.put(pool.remove(random.nextInt(pool.size())), 0.5 + random.nextDouble());
        }
        return new CompanionData(UUID.randomUUID(), species, rarity, round(h), round(d), round(s), traits);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /**
     * Adds a freshly hatched companion to {@code owner}'s collection (owner may be offline).
     * Returns false if the collection is full.
     */
    public static boolean hatchInto(MinecraftServer server, UUID owner, EggRarity rarity, BlockPos pos, ServerWorld world) {
        HunterProfile prof = ProfileState.get(server).of(owner);
        if (prof.companions.size() >= SanctumConfig.companionSlots) return false;
        CompanionData data = roll(world.getRandom(), rarity);
        prof.companions.add(data);
        if (prof.activeCompanion == null) prof.activeCompanion = data.id;
        ProfileState.get(server).markDirty();

        world.spawnParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.4, 0.3, 0.4, 0);
        world.playSound(null, pos, SoundEvents.BLOCK_SNIFFER_EGG_HATCH, SoundCategory.BLOCKS, 1.0F, 1.0F);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
        if (player != null) {
            player.sendMessage(Text.translatable("message.sanctumcraft.companion.hatched", describe(data), traitsText(data))
                    .formatted(Formatting.GREEN));
            if (!prof.whistleGiven) {
                prof.whistleGiven = true;
                ItemStack whistle = new ItemStack(ModItems.COMPANION_WHISTLE);
                if (!player.getInventory().insertStack(whistle)) player.dropItem(whistle, false);
                player.sendMessage(Text.translatable("message.sanctumcraft.companion.whistle_given").formatted(Formatting.GRAY));
            }
        }
        return true;
    }

    public static MutableText describe(CompanionData d) {
        return Text.translatable("entity.sanctumcraft.companion", Text.translatable(d.rarity.key()),
                Text.translatable(d.species.key())).formatted(d.rarity.color);
    }

    public static MutableText traitsText(CompanionData d) {
        MutableText t = Text.empty();
        int i = 0;
        for (Map.Entry<CompanionTrait, Double> e : d.traits.entrySet()) {
            if (i++ > 0) t.append(", ");
            t.append(Text.translatable(e.getKey().key())).append(" " + Math.round(e.getValue() * 100) + " %");
        }
        return t;
    }

    // ------------------------------------------------------------------ collection

    public static CompanionData find(HunterProfile p, UUID id) {
        for (CompanionData d : p.companions) if (d.id.equals(id)) return d;
        return null;
    }

    public static long koTicks(EggRarity r) {
        return switch (r) {
            case COMMON -> 3 * 1200;
            case RARE -> 4 * 1200;
            case EPIC -> 6 * 1200;
            case LEGENDARY -> 8 * 1200;
        };
    }

    public static void setActive(ServerPlayerEntity player, UUID id) {
        despawn(player);
        Profiles.of(player).activeCompanion = id;
        Profiles.dirty(player);
        player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1.0F, 1.0F);
    }

    /** Right-click release: needs a second right-click within 5 s. */
    public static boolean requestRelease(ServerPlayerEntity player, UUID id) {
        if (!id.equals(PENDING_RELEASE.get(player.getUuid()))) {
            PENDING_RELEASE.put(player.getUuid(), id);
            player.sendMessage(Text.translatable("message.sanctumcraft.companion.release_confirm").formatted(Formatting.RED), true);
            return false;
        }
        PENDING_RELEASE.remove(player.getUuid());
        HunterProfile p = Profiles.of(player);
        CompanionData d = find(p, id);
        if (d == null) return false;
        if (id.equals(p.activeCompanion)) {
            despawn(player);
            p.activeCompanion = null;
        }
        p.companions.remove(d);
        int shells = switch (d.rarity) {
            case COMMON -> 25;
            case RARE -> 60;
            case EPIC -> 150;
            case LEGENDARY -> 400;
        };
        Profiles.addShells(player, shells);
        Profiles.addHunterXp(player, shells / 2);
        player.sendMessage(Text.translatable("message.sanctumcraft.companion.released", describe(d), shells)
                .formatted(Formatting.GRAY));
        return true;
    }

    // ------------------------------------------------------------------ summoning

    private static long now(MinecraftServer server) {
        return server.getOverworld().getTime();
    }

    public static MobEntity summoned(ServerPlayerEntity player) {
        UUID id = SUMMONED.get(player.getUuid());
        if (id == null || player.getServer() == null) return null;
        for (ServerWorld w : player.getServer().getWorlds()) {
            Entity e = w.getEntity(id);
            if (e instanceof MobEntity m && m.isAlive()) return m;
        }
        SUMMONED.remove(player.getUuid());
        OWNER_OF.remove(id);
        return null;
    }

    public static void whistle(ServerPlayerEntity player) {
        if (summoned(player) != null) {
            despawn(player);
            player.sendMessage(Text.translatable("message.sanctumcraft.companion.recalled").formatted(Formatting.GRAY), true);
            return;
        }
        HunterProfile p = Profiles.of(player);
        CompanionData d = p.activeCompanion != null ? find(p, p.activeCompanion) : null;
        if (d == null) {
            player.sendMessage(Text.translatable("message.sanctumcraft.companion.none").formatted(Formatting.GRAY), true);
            return;
        }
        long ko = d.koUntil - now(player.getServer());
        if (ko > 0) {
            player.sendMessage(Text.translatable("message.sanctumcraft.companion.ko", InstanceManager.formatTime(ko))
                    .formatted(Formatting.RED), true);
            return;
        }
        summon(player, d, player.getPos());
    }

    public static void summon(ServerPlayerEntity owner, CompanionData d, Vec3d pos) {
        ServerWorld world = owner.getServerWorld();
        MobEntity mob = d.species.type.create(world);
        if (mob == null) return;
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, owner.getYaw(), 0F);
        mob.addCommandTag(TAG);
        mob.addCommandTag("sc_cid_" + d.id);
        mob.setCustomName(describe(d));
        mob.setCustomNameVisible(false);
        mob.setCanPickUpLoot(false);

        UUID ownerId = owner.getUuid();
        if (mob instanceof TameableEntity tame) {
            tame.setTamed(true, true);
            tame.setOwner(owner);
        }
        if (mob instanceof WolfEntity wolf) {
            world.getRegistryManager().get(RegistryKeys.WOLF_VARIANT).getEntry(variant(d.rarity, d.id))
                    .ifPresent(wolf::setVariant);
        }
        if (mob instanceof IronGolemEntity golem) {
            golem.setPlayerCreated(true);
        }
        if (mob instanceof PathAwareEntity paw && !(mob instanceof TameableEntity)) {
            MobEntityAccessor acc = (MobEntityAccessor) mob;
            if (d.species == CompanionSpecies.FOX) {
                // Fox: replace its wild AI (fleeing players, hunting chickens, sleeping...).
                acc.sanctumcraft$getGoalSelector().clear(g -> true);
                acc.sanctumcraft$getTargetSelector().clear(g -> true);
                acc.sanctumcraft$getGoalSelector().add(0, new SwimGoal(mob));
                acc.sanctumcraft$getGoalSelector().add(1, new MeleeAttackGoal(paw, 1.3, true));
                acc.sanctumcraft$getGoalSelector().add(6, new LookAroundGoal(mob));
            }
            acc.sanctumcraft$getGoalSelector().add(2, new CompanionFollowGoal(mob,
                    () -> owner.getServer() != null ? owner.getServer().getPlayerManager().getPlayer(ownerId) : null));
        }

        double health = d.health * (1 + 0.5 * d.power(CompanionTrait.STURDY)) - 1;
        double damage = d.damage * (1 + 0.25 * d.power(CompanionTrait.FEROCIOUS)) - 1;
        double speed = d.speed * (1 + 0.20 * d.power(CompanionTrait.SWIFT)) - 1;
        modifier(mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH), HEALTH_ID, health);
        modifier(mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DAMAGE_ID, damage);
        modifier(mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), SPEED_ID, speed);
        mob.setHealth(mob.getMaxHealth());

        SUMMONED.put(ownerId, mob.getUuid());
        OWNER_OF.put(mob.getUuid(), ownerId);
        world.spawnEntity(mob);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 10, 0.3, 0.3, 0.3, 0.02);
        world.playSound(null, BlockPos.ofFloored(pos), SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    private static RegistryKey<WolfVariant> variant(EggRarity r, UUID seed) {
        List<RegistryKey<WolfVariant>> l = switch (r) {
            case COMMON -> List.of(WolfVariants.PALE, WolfVariants.WOODS, WolfVariants.ASHEN);
            case RARE -> List.of(WolfVariants.CHESTNUT, WolfVariants.RUSTY, WolfVariants.SPOTTED);
            case EPIC -> List.of(WolfVariants.STRIPED, WolfVariants.BLACK);
            case LEGENDARY -> List.of(WolfVariants.SNOWY);
        };
        return l.get(Math.floorMod(seed.hashCode(), l.size()));
    }

    private static void modifier(EntityAttributeInstance inst, Identifier id, double value) {
        if (inst == null || Math.abs(value) < 1e-6) return;
        inst.overwritePersistentModifier(new EntityAttributeModifier(id, value,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Removes the summoned companion (no KO). Returns true if one was out. */
    public static boolean despawn(ServerPlayerEntity player) {
        MobEntity m = summoned(player);
        if (m == null) return false;
        giveBackHeld(player, m);
        SUMMONED.remove(player.getUuid());
        OWNER_OF.remove(m.getUuid());
        ((ServerWorld) m.getWorld()).spawnParticles(ParticleTypes.CLOUD, m.getX(), m.getY() + 0.5, m.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        m.discard();
        return true;
    }

    private static CompanionData activeData(ServerPlayerEntity player) {
        HunterProfile p = Profiles.of(player);
        return p.activeCompanion != null ? find(p, p.activeCompanion) : null;
    }

    // ------------------------------------------------------------------ events

    public static void register() {
        // Companions can't be given items (the allay would take the whistle) nor be interacted with;
        // right-clicking your companion with the whistle calls it back.
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!entity.getCommandTags().contains(TAG)) return net.minecraft.util.ActionResult.PASS;
            // Never hand an item to a companion.
            if (!world.isClient && player instanceof ServerPlayerEntity sp
                    && player.getStackInHand(hand).isOf(ModItems.COMPANION_WHISTLE)) {
                whistle(sp);
            }
            return net.minecraft.util.ActionResult.SUCCESS;
        });

        // Defeated companion: no death, no drops; knocked out for a while.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            UUID owner = OWNER_OF.get(entity.getUuid());
            if (owner == null || !entity.getCommandTags().contains(TAG)) return true;
            MinecraftServer server = entity.getServer();
            if (server == null) return true;
            HunterProfile prof = ProfileState.get(server).of(owner);
            for (String tag : entity.getCommandTags()) {
                if (!tag.startsWith("sc_cid_")) continue;
                CompanionData d = find(prof, UUID.fromString(tag.substring(7)));
                if (d != null) {
                    d.koUntil = now(server) + koTicks(d.rarity);
                    ProfileState.get(server).markDirty();
                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(owner);
                    if (p != null) {
                        p.sendMessage(Text.translatable("message.sanctumcraft.companion.knocked_out", describe(d),
                                InstanceManager.formatTime(koTicks(d.rarity))).formatted(Formatting.RED));
                    }
                }
            }
            SUMMONED.remove(owner);
            OWNER_OF.remove(entity.getUuid());
            ((ServerWorld) entity.getWorld()).spawnParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.5,
                    entity.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            entity.discard();
            return false;
        });

        // A companion saved in a chunk (server stop...) is never "loaded back": remove it.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(TAG) && !OWNER_OF.containsKey(entity.getUuid())) {
                entity.discard();
            }
        });

        // The companion follows its owner through any dimension change (sanctum portals included).
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (despawn(player)) {
                CompanionData d = activeData(player);
                if (d != null) summon(player, d, player.getPos());
            }
        });
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        despawn(player);
        PENDING_RELEASE.remove(player.getUuid());
    }

    // ------------------------------------------------------------------ every second

    /**
     * Safety net: whatever a companion holds (hands, and the allay's own inventory) goes back to its
     * owner; and a companion is never in love mode (no breeding).
     */
    private static void giveBackHeld(ServerPlayerEntity owner, MobEntity mob) {
        List<ItemStack> back = new ArrayList<>();
        for (net.minecraft.util.Hand hand : net.minecraft.util.Hand.values()) {
            ItemStack held = mob.getStackInHand(hand);
            if (held.isEmpty()) continue;
            mob.setStackInHand(hand, ItemStack.EMPTY);
            back.add(held);
        }
        if (mob instanceof net.minecraft.entity.InventoryOwner io) back.addAll(io.getInventory().clearToList());
        for (ItemStack st : back) {
            if (st.isEmpty()) continue;
            if (!owner.getInventory().insertStack(st)) owner.dropItem(st, false);
        }
        if (mob instanceof net.minecraft.entity.passive.AnimalEntity animal && animal.isInLove()) animal.resetLoveTicks();
    }

    public static void tick(ServerPlayerEntity owner, SanctumInstance inst, int serverTicks) {
        MobEntity mob = summoned(owner);
        if (mob == null) return;
        giveBackHeld(owner, mob);
        if (!owner.isAlive() || mob.getWorld() != owner.getWorld()) {
            despawn(owner);
            return;
        }
        double dist = mob.squaredDistanceTo(owner);
        if (dist > 24 * 24) {
            mob.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), mob.getYaw(), mob.getPitch());
            mob.getNavigation().stop();
        } else if (mob.getType() == CompanionSpecies.ALLAY.type && dist > 36) {
            mob.getNavigation().startMovingTo(owner, 1.2);
        }
        // Defend the owner.
        LivingEntity threat = owner.getAttacker();
        if (threat == null || !threat.isAlive() || threat == mob) {
            LivingEntity attacked = owner.getAttacking();
            threat = attacked instanceof HostileEntity && attacked.isAlive() ? attacked : null;
        }
        // The allay has no attack: it only supports.
        boolean fighter = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null;
        if (fighter && threat != null && !(threat instanceof PlayerEntity) && mob.getTarget() == null) mob.setTarget(threat);

        if (inst == null) return;
        CompanionData d = activeData(owner);
        if (d == null) return;
        double heal = d.power(CompanionTrait.HEALER);
        if (heal > 0 && serverTicks % 200 == 0 && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal((float) (2.0 * heal));
            owner.getServerWorld().spawnParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2, owner.getZ(), 2, 0.3, 0.1, 0.3, 0);
        }
        if (d.power(CompanionTrait.GUARDIAN) > 0 && dist < 16 * 16) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
        }
        double track = d.power(CompanionTrait.TRACKER);
        if (track > 0) {
            int interval = Math.max(100, (int) Math.round(400 / track / 20) * 20);
            if (serverTicks % interval == 0) {
                pointToNest(owner, inst, Text.translatable("message.sanctumcraft.companion.tracker"));
            }
        }
    }

    // ------------------------------------------------------------------ nest direction (tracker trait, night eyes)

    public static void pointToNest(ServerPlayerEntity player, SanctumInstance inst, Text prefix) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlacedRoom r : inst.rooms) {
            if (r.kind() != RoomKind.NEST) continue;
            BlockPos nest = inst.roomOrigin(r).add(12, 3, 12);
            var st = player.getServerWorld().getBlockState(nest);
            if (!st.isOf(ModBlocks.EGG_NEST) || !st.get(EggNestBlock.ACTIVE)) continue;
            double dd = nest.getSquaredDistance(player.getPos());
            if (dd < bestDist) {
                bestDist = dd;
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

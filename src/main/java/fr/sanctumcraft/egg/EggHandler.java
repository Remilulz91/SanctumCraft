package fr.sanctumcraft.egg;

import fr.sanctumcraft.block.EggNestBlock;
import fr.sanctumcraft.config.SanctumConfig;
import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.SanctumInstance;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.registry.ModDimensions;
import fr.sanctumcraft.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Nest digging, egg weight, egg loss on death and egg extraction. */
public final class EggHandler {

    private static final int HIT_COOLDOWN_TICKS = 4;
    private static final Map<UUID, Integer> LAST_HIT = new HashMap<>();

    private EggHandler() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            BlockState state = world.getBlockState(pos);
            if (!state.isOf(ModBlocks.EGG_NEST) || !state.get(EggNestBlock.ACTIVE)) return ActionResult.PASS;
            if (player.isSpectator()) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                hitNest(sp, sw, pos);
            }
            return ActionResult.SUCCESS;
        });

        // Eggs never survive a death in the sanctum, even with keepInventory / sanctumKeepInventory:
        // whatever is still in the inventory after the death drop is thrown on the ground.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player && ModDimensions.isSanctum(player.getWorld())) {
                PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (stack.getItem() instanceof SanctumEggItem) {
                        player.dropItem(stack.copy(), true, false);
                        inv.setStack(i, ItemStack.EMPTY);
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------ nests

    private static void hitNest(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        SanctumInstance inst = InstanceManager.instanceAt(world, pos);
        if (inst == null || inst.status != SanctumInstance.Status.ACTIVE) return;
        int now = world.getServer().getTicks();
        Integer last = LAST_HIT.get(player.getUuid());
        if (last != null && now - last < HIT_COOLDOWN_TICKS) return;
        LAST_HIT.put(player.getUuid(), now);

        int hits = inst.nestHits.merge(pos.asLong(), 1, Integer::sum);
        int needed = Math.max(1, SanctumConfig.nestHits);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.1);
        world.playSound(null, pos, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.BLOCKS, 1.0F,
                0.8F + 0.4F * hits / needed);

        if (hits < needed) {
            player.sendMessage(Text.translatable("message.sanctumcraft.nest.digging", bar(hits, needed))
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        player.sendMessage(Text.translatable("message.sanctumcraft.nest.digging", bar(needed, needed))
                .formatted(Formatting.YELLOW), true);
        inst.nestHits.remove(pos.asLong());
        world.setBlockState(pos, world.getBlockState(pos).with(EggNestBlock.ACTIVE, false));
        EggRarity rarity = EggRarity.roll(world.getRandom(), inst.difficulty);
        ItemStack egg = new ItemStack(ModItems.eggFor(rarity));
        if (!player.getInventory().insertStack(egg)) {
            player.dropItem(egg, false);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_SNIFFER_EGG_PLOP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                20, 0.4, 0.4, 0.4, 0.0);
        Text eggName = Text.translatable(ModItems.eggFor(rarity).getTranslationKey()).formatted(rarity.color);
        for (ServerPlayerEntity p : InstanceManager.playersInside(world.getServer(), inst)) {
            p.sendMessage(Text.translatable("message.sanctumcraft.nest.found", player.getDisplayName(), eggName)
                    .formatted(Formatting.GOLD));
        }
    }

    private static String bar(int value, int max) {
        int filled = Math.round(10f * value / max);
        return "■".repeat(filled) + "□".repeat(10 - filled);
    }

    // ------------------------------------------------------------------ carrying

    /** Called every second for players inside an active sanctum. */
    public static void applyWeight(ServerPlayerEntity player) {
        int eggs = 0;
        int weight = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).getItem() instanceof SanctumEggItem egg) {
                eggs++;
                weight = Math.max(weight, egg.rarity.weight);
            }
        }
        if (eggs == 0) return;
        int amplifier = Math.min(3, weight + eggs - 1);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, amplifier, true, false, true));
    }

    // ------------------------------------------------------------------ extraction

    /** Called when a player leaves a sanctum alive: every unsecured egg becomes secured and pays XP. */
    public static void extract(ServerPlayerEntity player, SanctumInstance inst) {
        PlayerInventory inv = player.getInventory();
        int count = 0;
        int xp = 0;
        java.util.List<EggRarity> secured = new java.util.ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() instanceof SanctumEggItem egg && !SanctumEggItem.isSecured(stack)) {
                SanctumEggItem.secure(stack);
                count++;
                secured.add(egg.rarity);
                xp += Math.round(egg.rarity.extractionXp * inst.difficulty.loot);
            }
        }
        if (count == 0) return;
        player.addExperience(xp);
        long shells = fr.sanctumcraft.profile.Profiles.onExtraction(player, secured, inst.difficulty);
        player.sendMessage(Text.translatable("message.sanctumcraft.extraction_reward", count, shells)
                .formatted(Formatting.GOLD));
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.translatable("subtitle.sanctumcraft.extracted", count, xp).formatted(Formatting.GRAY)));
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.translatable("title.sanctumcraft.extracted").formatted(Formatting.GOLD, Formatting.BOLD)));
        player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0F, 1.0F);
    }
}

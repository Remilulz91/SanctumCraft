package fr.sanctumcraft.incubator;

import fr.sanctumcraft.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/** Holds the incubating egg: rarity, start time and duration (overworld game time). */
public class IncubatorBlockEntity extends BlockEntity {

    /** Egg rarity name, or empty when the incubator is empty. */
    String rarity = "";
    long start;
    long duration;
    /** Owner of the egg: the companion goes to this player's collection. */
    java.util.UUID owner;
    String ownerName = "";

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.INCUBATOR_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("rarity", rarity);
        nbt.putLong("start", start);
        nbt.putLong("duration", duration);
        if (owner != null) nbt.putUuid("owner", owner);
        nbt.putString("ownerName", ownerName);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        rarity = nbt.getString("rarity");
        start = nbt.getLong("start");
        duration = nbt.getLong("duration");
        owner = nbt.containsUuid("owner") ? nbt.getUuid("owner") : null;
        ownerName = nbt.getString("ownerName");
    }

    /** Server ticker: switches to "ready" as soon as the time is up, and tells the owner. */
    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, IncubatorBlockEntity be) {
        if (world.getTime() % 20 != 0 || be.rarity.isEmpty()) return;
        if (state.get(IncubatorBlock.STAGE) != 1 || world.getServer() == null) return;
        if (world.getServer().getOverworld().getTime() < be.start + be.duration) return;
        world.setBlockState(pos, state.with(IncubatorBlock.STAGE, 2), net.minecraft.block.Block.NOTIFY_ALL);
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_SNIFFER_EGG_CRACK,
                net.minecraft.sound.SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (world instanceof net.minecraft.server.world.ServerWorld sw) {
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1,
                    pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0);
        }
        if (be.owner != null) {
            var p = world.getServer().getPlayerManager().getPlayer(be.owner);
            if (p != null) {
                p.sendMessage(net.minecraft.text.Text.translatable("message.sanctumcraft.incubator.ready",
                        pos.getX(), pos.getY(), pos.getZ()).formatted(net.minecraft.util.Formatting.GREEN));
            }
        }
    }
}

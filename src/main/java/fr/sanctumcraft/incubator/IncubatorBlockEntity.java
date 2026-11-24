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

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.INCUBATOR_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("rarity", rarity);
        nbt.putLong("start", start);
        nbt.putLong("duration", duration);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        rarity = nbt.getString("rarity");
        start = nbt.getLong("start");
        duration = nbt.getLong("duration");
    }
}

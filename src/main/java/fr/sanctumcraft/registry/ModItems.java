package fr.sanctumcraft.registry;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.portal.SanctumShardItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public final class ModItems {

    /** Consumed to light a rune stone frame into a sanctum portal. */
    public static final Item SANCTUM_SHARD = Registry.register(Registries.ITEM, SanctumCraft.id("sanctum_shard"),
            new SanctumShardItem(new Item.Settings().maxCount(16).rarity(Rarity.RARE)));

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, SanctumCraft.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(SANCTUM_SHARD))
                    .displayName(Text.translatable("itemGroup.sanctumcraft.main"))
                    .entries((context, entries) -> {
                        entries.add(ModBlocks.RUNE_STONE);
                        entries.add(SANCTUM_SHARD);
                    })
                    .build());

    private ModItems() {}

    public static void register() {}
}

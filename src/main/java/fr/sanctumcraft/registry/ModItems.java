package fr.sanctumcraft.registry;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.egg.EggRarity;
import fr.sanctumcraft.egg.SanctumEggItem;
import fr.sanctumcraft.loot.KeyTier;
import fr.sanctumcraft.loot.SanctumKeyItem;
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

    /** Unlocks one Chaos sanctum. Consumed when the Chaos difficulty is chosen. */
    public static final Item CHAOS_TICKET = Registry.register(Registries.ITEM, SanctumCraft.id("chaos_ticket"),
            new Item(new Item.Settings().maxCount(16).rarity(Rarity.EPIC)));

    public static final SanctumEggItem EGG_COMMON = egg("sanctum_egg_common", EggRarity.COMMON);
    public static final SanctumEggItem EGG_RARE = egg("sanctum_egg_rare", EggRarity.RARE);
    public static final SanctumEggItem EGG_EPIC = egg("sanctum_egg_epic", EggRarity.EPIC);
    public static final SanctumEggItem EGG_LEGENDARY = egg("sanctum_egg_legendary", EggRarity.LEGENDARY);

    public static final SanctumKeyItem RUSTED_KEY = key("rusted_key", KeyTier.RUSTED);
    public static final SanctumKeyItem SILVER_KEY = key("silver_key", KeyTier.SILVER);
    public static final SanctumKeyItem GILDED_KEY = key("gilded_key", KeyTier.GILDED);

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, SanctumCraft.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(SANCTUM_SHARD))
                    .displayName(Text.translatable("itemGroup.sanctumcraft.main"))
                    .entries((context, entries) -> {
                        entries.add(ModBlocks.RUNE_STONE);
                        entries.add(SANCTUM_SHARD);
                        entries.add(CHAOS_TICKET);
                        entries.add(EGG_COMMON);
                        entries.add(EGG_RARE);
                        entries.add(EGG_EPIC);
                        entries.add(EGG_LEGENDARY);
                        entries.add(RUSTED_KEY);
                        entries.add(SILVER_KEY);
                        entries.add(GILDED_KEY);
                        entries.add(ModBlocks.LOOT_SPOT);
                    })
                    .build());

    private ModItems() {}

    private static SanctumEggItem egg(String name, EggRarity rarity) {
        return Registry.register(Registries.ITEM, SanctumCraft.id(name),
                new SanctumEggItem(rarity, new Item.Settings().maxCount(1).fireproof()));
    }

    private static SanctumKeyItem key(String name, KeyTier tier) {
        return Registry.register(Registries.ITEM, SanctumCraft.id(name),
                new SanctumKeyItem(tier, new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
    }

    public static SanctumKeyItem keyFor(KeyTier tier) {
        return switch (tier) {
            case RUSTED -> RUSTED_KEY;
            case SILVER -> SILVER_KEY;
            case GILDED -> GILDED_KEY;
        };
    }

    public static SanctumEggItem eggFor(EggRarity rarity) {
        return switch (rarity) {
            case COMMON -> EGG_COMMON;
            case RARE -> EGG_RARE;
            case EPIC -> EGG_EPIC;
            case LEGENDARY -> EGG_LEGENDARY;
        };
    }

    public static void register() {}
}

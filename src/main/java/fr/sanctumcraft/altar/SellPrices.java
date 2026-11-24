package fr.sanctumcraft.altar;

import fr.sanctumcraft.egg.SanctumEggItem;
import fr.sanctumcraft.registry.ModBlocks;
import fr.sanctumcraft.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/** What the altar buys back (per item). Eggs are never sellable. */
public final class SellPrices {

    private static final Map<Item, Integer> PRICES = new HashMap<>();

    static {
        PRICES.put(ModItems.RUSTED_KEY, 200);
        PRICES.put(ModItems.SILVER_KEY, 500);
        PRICES.put(ModItems.GILDED_KEY, 1200);
        PRICES.put(ModItems.SANCTUM_SHARD, 120);
        PRICES.put(ModBlocks.RUNE_STONE.asItem(), 10);
        PRICES.put(Items.DIAMOND, 60);
        PRICES.put(Items.NETHERITE_SCRAP, 300);
        PRICES.put(Items.EMERALD, 20);
        PRICES.put(Items.GOLD_INGOT, 10);
        PRICES.put(Items.IRON_INGOT, 5);
        PRICES.put(Items.GOLDEN_APPLE, 60);
        PRICES.put(Items.ENCHANTED_GOLDEN_APPLE, 1000);
        PRICES.put(Items.TOTEM_OF_UNDYING, 400);
        PRICES.put(Items.EXPERIENCE_BOTTLE, 10);
        PRICES.put(Items.ENCHANTED_BOOK, 80);
        PRICES.put(Items.ENDER_PEARL, 15);
        PRICES.put(Items.AMETHYST_SHARD, 2);
        PRICES.put(Items.LAPIS_LAZULI, 2);
        PRICES.put(Items.REDSTONE, 1);
    }

    private SellPrices() {}

    /** Total value of a stack, or 0 if the altar doesn't buy it. */
    public static long value(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof SanctumEggItem) return 0;
        Integer p = PRICES.get(stack.getItem());
        return p == null ? 0 : (long) p * stack.getCount();
    }
}

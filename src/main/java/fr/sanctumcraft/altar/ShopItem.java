package fr.sanctumcraft.altar;

import fr.sanctumcraft.registry.ModItems;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

import java.util.List;
import java.util.function.Supplier;

/** Fixed shop catalogue (the Chaos Ticket is handled separately: its price grows). */
public record ShopItem(Supplier<ItemStack> stack, long price) {

    public static List<ShopItem> catalogue() {
        return List.of(
                new ShopItem(() -> new ItemStack(ModItems.RUSTED_KEY), 600),
                new ShopItem(() -> new ItemStack(ModItems.SILVER_KEY), 1500),
                new ShopItem(() -> new ItemStack(ModItems.GILDED_KEY), 3500),
                new ShopItem(() -> new ItemStack(ModItems.SANCTUM_SHARD), 400),
                new ShopItem(() -> new ItemStack(fr.sanctumcraft.registry.ModBlocks.RUNE_STONE, 8), 200),
                new ShopItem(() -> PotionContentsComponent.createStack(Items.POTION, Potions.STRONG_HEALING), 150),
                new ShopItem(() -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.STRONG_HEALING), 220),
                new ShopItem(() -> PotionContentsComponent.createStack(Items.POTION, Potions.LONG_STRENGTH), 250),
                new ShopItem(() -> new ItemStack(Items.GOLDEN_APPLE), 300),
                new ShopItem(() -> new ItemStack(Items.ARROW, 32), 80),
                new ShopItem(() -> new ItemStack(Items.TORCH, 32), 40),
                new ShopItem(() -> new ItemStack(Items.COOKED_BEEF, 16), 60));
    }
}

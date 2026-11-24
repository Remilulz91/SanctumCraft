package fr.sanctumcraft.loot;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

/** Runtime state of an opened sanctum chest, shared by everyone looking into it. */
public final class LootChest {

    private static final String PLACEHOLDER = "sanctumcraft_placeholder";

    public final ChestTier tier;
    public final SimpleInventory inventory = new SimpleInventory(27);
    /** Items still hidden, in reveal order, with their target slot. */
    final List<ItemStack> pending = new ArrayList<>();
    final List<Integer> pendingSlots = new ArrayList<>();
    int timer;
    long lastTick = -1;
    int total;

    public LootChest(ChestTier tier) {
        this.tier = tier;
    }

    public boolean isRevealing() {
        return !pending.isEmpty();
    }

    public int revealed() {
        return total - pending.size();
    }

    public static ItemStack placeholder() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("menu.sanctumcraft.chest.searching")
                .formatted(Formatting.DARK_GRAY).styled(s -> s.withItalic(false)));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(PLACEHOLDER, true));
        return stack;
    }

    public static boolean isPlaceholder(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(PLACEHOLDER);
    }

    /** Search time of an item, in ticks: rarer items take longer (like the blue gauge of the original). */
    static int revealTicks(ItemStack stack) {
        Rarity r = stack.getRarity();
        return switch (r) {
            case COMMON -> 8;
            case UNCOMMON -> 14;
            case RARE -> 22;
            case EPIC -> 32;
        };
    }
}

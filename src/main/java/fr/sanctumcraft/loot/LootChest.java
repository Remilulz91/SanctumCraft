package fr.sanctumcraft.loot;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    /**
     * Search time of an item, in ticks: rarer items and rarer chests take longer
     * (like the search gauge of the original). Basic chest + common item = 1 s.
     */
    static int revealTicks(ItemStack stack, ChestTier tier) {
        int base = switch (stack.getRarity()) {
            case COMMON -> 20;
            case UNCOMMON -> 30;
            case RARE -> 45;
            case EPIC -> 60;
        };
        float chestFactor = switch (tier) {
            case BASIC -> 1.0F;
            case FINE -> 1.2F;
            case RARE -> 1.45F;
            case EPIC -> 1.75F;
            case LEGENDARY -> 2.1F;
        };
        return Math.round(base * chestFactor);
    }
}

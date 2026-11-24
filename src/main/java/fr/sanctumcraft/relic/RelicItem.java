package fr.sanctumcraft.relic;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A relic. The collector version (rare) shines, has a gold name and fits in the showcase. */
public class RelicItem extends Item {

    private static final String COLLECTOR = "collector";

    public final Relic relic;

    public RelicItem(Relic relic, Settings settings) {
        super(settings);
        this.relic = relic;
    }

    public static boolean isCollector(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(COLLECTOR);
    }

    public static ItemStack collector(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(COLLECTOR, true));
        return stack;
    }

    @Override
    public Text getName(ItemStack stack) {
        return isCollector(stack)
                ? Text.translatable("item.sanctumcraft.relic.collector", Text.translatable(getTranslationKey()))
                        .formatted(Formatting.GOLD)
                : Text.translatable(getTranslationKey()).formatted(Formatting.YELLOW);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return isCollector(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.sanctumcraft.relic.row" + relic.row).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(isCollector(stack) ? "tooltip.sanctumcraft.relic.collector"
                : "tooltip.sanctumcraft.relic.normal").formatted(Formatting.DARK_GRAY));
    }
}

package fr.sanctumcraft.loot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Opens the vault doors of its tier. Survives outside the sanctum: keep it for a later run. */
public class SanctumKeyItem extends Item {

    public final KeyTier tier;

    public SanctumKeyItem(KeyTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(getTranslationKey()).formatted(tier.color);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.sanctumcraft.key." + tier.name().toLowerCase()).formatted(Formatting.GRAY));
    }
}

package fr.sanctumcraft.egg;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Egg dug out of a sanctum nest. Heavy (slows its carrier inside the sanctum) and
 * lost on death in the sanctum until it is extracted ("secured").
 * Hatching comes with the incubator (0.6).
 */
public class SanctumEggItem extends Item {

    private static final String SECURED = "secured";

    public final EggRarity rarity;

    public SanctumEggItem(EggRarity rarity, Settings settings) {
        super(settings);
        this.rarity = rarity;
    }

    public static boolean isSecured(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(SECURED);
    }

    public static void secure(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(SECURED, true));
    }

    /** Secures the egg and binds it to the player who extracted it. */
    public static void secure(ItemStack stack, net.minecraft.entity.player.PlayerEntity owner) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putBoolean(SECURED, true);
            nbt.putUuid("owner", owner.getUuid());
            nbt.putString("ownerName", owner.getName().getString());
        });
    }

    public static java.util.UUID owner(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        var nbt = data.copyNbt();
        return nbt.containsUuid("owner") ? nbt.getUuid("owner") : null;
    }

    public static String ownerName(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? "" : data.copyNbt().getString("ownerName");
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(getTranslationKey()).formatted(rarity.color);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return rarity == EggRarity.LEGENDARY || super.hasGlint(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(rarity.key()).formatted(rarity.color));
        if (isSecured(stack)) {
            tooltip.add(Text.translatable("tooltip.sanctumcraft.egg.secured").formatted(Formatting.GREEN));
            if (!ownerName(stack).isEmpty()) {
                tooltip.add(Text.translatable("tooltip.sanctumcraft.egg.owner", ownerName(stack)).formatted(Formatting.GRAY));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.sanctumcraft.egg.unsecured").formatted(Formatting.RED));
        }
        tooltip.add(Text.translatable("tooltip.sanctumcraft.egg.heavy").formatted(Formatting.DARK_GRAY));
    }
}

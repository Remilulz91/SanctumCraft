package fr.sanctumcraft.guide;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.profile.HunterProfile;
import fr.sanctumcraft.profile.Profiles;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The Sanctum Guide is a Patchouli book (sanctumcraft:guide, see tools/gen_guide.py).
 * Given once on first join, and again with /sanctum guide. The stack is built from the registries
 * (patchouli:guide_book + the patchouli:book component), so no compile-time dependency on Patchouli.
 */
public final class SanctumGuide {

    public static final Identifier BOOK_ID = SanctumCraft.id("guide");
    private static final Identifier BOOK_ITEM = Identifier.of("patchouli", "guide_book");
    private static final Identifier BOOK_COMPONENT = Identifier.of("patchouli", "book");

    private SanctumGuide() {}

    /** The guide book, or EMPTY if Patchouli is missing. */
    @SuppressWarnings("unchecked")
    public static ItemStack create() {
        Item item = Registries.ITEM.get(BOOK_ITEM);
        ComponentType<?> type = Registries.DATA_COMPONENT_TYPE.get(BOOK_COMPONENT);
        if (item == Items.AIR || type == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.set((ComponentType<Identifier>) type, BOOK_ID);
        return stack;
    }

    public static boolean give(ServerPlayerEntity player) {
        ItemStack book = create();
        if (book.isEmpty()) {
            player.sendMessage(Text.translatable("message.sanctumcraft.guide.missing").formatted(Formatting.RED), false);
            return false;
        }
        if (!player.getInventory().insertStack(book)) player.dropItem(book, false);
        player.sendMessage(Text.translatable("message.sanctumcraft.guide.given").formatted(Formatting.LIGHT_PURPLE), false);
        return true;
    }

    /** First join: give the guide once. */
    public static void onJoin(ServerPlayerEntity player) {
        HunterProfile p = Profiles.of(player);
        if (!p.guideGiven && give(player)) {
            p.guideGiven = true;
            Profiles.dirty(player);
        }
    }
}

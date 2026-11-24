package fr.sanctumcraft.menu;

import fr.sanctumcraft.instance.InstanceManager;
import fr.sanctumcraft.instance.SanctumDifficulty;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * Chest-style menu (server side only, vanilla client screen) shown when a player
 * enters a free portal: pick Normal / Nightmare / Chaos.
 */
public class DifficultyMenu extends GenericContainerScreenHandler {

    private static final int SLOT_NORMAL = 11;
    private static final int SLOT_NIGHTMARE = 13;
    private static final int SLOT_CHAOS = 15;

    private final UUID portalId;
    private final ServerPlayerEntity owner;
    private boolean chosen;

    private DifficultyMenu(int syncId, PlayerInventory playerInventory, ServerPlayerEntity owner, UUID portalId) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, buildInventory(owner), 3);
        this.portalId = portalId;
        this.owner = owner;
    }

    public static void open(ServerPlayerEntity player, UUID portalId) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new DifficultyMenu(syncId, inv, player, portalId),
                Text.translatable("menu.sanctumcraft.difficulty.title")));
    }

    private static SimpleInventory buildInventory(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(27);
        ItemStack filler = new ItemStack(Items.PURPLE_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        for (int i = 0; i < 27; i++) inv.setStack(i, filler.copy());

        inv.setStack(SLOT_NORMAL, icon(Items.IRON_SWORD, SanctumDifficulty.NORMAL, null));
        inv.setStack(SLOT_NIGHTMARE, icon(Items.WITHER_SKELETON_SKULL, SanctumDifficulty.NIGHTMARE, null));
        boolean ticket = InstanceManager.hasChaosTicket(player);
        inv.setStack(SLOT_CHAOS, icon(ticket ? Items.NETHER_STAR : Items.BARRIER, SanctumDifficulty.CHAOS,
                Text.translatable(ticket ? "menu.sanctumcraft.difficulty.chaos_ticket" : "menu.sanctumcraft.difficulty.chaos_locked")
                        .formatted(ticket ? Formatting.GOLD : Formatting.RED)));
        return inv;
    }

    private static ItemStack icon(net.minecraft.item.Item item, SanctumDifficulty d, Text extra) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.translatable(d.key()).formatted(d.color, Formatting.BOLD).styled(s -> s.withItalic(false)));
        Text time = Text.translatable("menu.sanctumcraft.difficulty.time",
                InstanceManager.formatTime(d.durationSeconds() * 20L)).formatted(Formatting.GRAY);
        Text desc = Text.translatable(d.key() + ".desc").formatted(Formatting.DARK_GRAY);
        stack.set(DataComponentTypes.LORE, extra == null
                ? new LoreComponent(List.of(time, desc))
                : new LoreComponent(List.of(time, desc, extra)));
        return stack;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Never let items move: this is a menu, not a container.
        if (!chosen && player == owner && actionType == SlotActionType.PICKUP) {
            SanctumDifficulty d = switch (slotIndex) {
                case SLOT_NORMAL -> SanctumDifficulty.NORMAL;
                case SLOT_NIGHTMARE -> SanctumDifficulty.NIGHTMARE;
                case SLOT_CHAOS -> SanctumDifficulty.CHAOS;
                default -> null;
            };
            if (d != null) {
                if (d == SanctumDifficulty.CHAOS && !InstanceManager.hasChaosTicket(owner)) {
                    owner.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0F, 1.0F);
                } else {
                    chosen = true;
                    owner.closeHandledScreen();
                    owner.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1.0F, 1.0F);
                    InstanceManager.openFromPortal(owner, portalId, d);
                    return;
                }
            }
        }
        syncState();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        // Do NOT call super: it would try to return the menu items to the player.
        if (!chosen && player instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.translatable("message.sanctumcraft.menu_closed").formatted(Formatting.GRAY), true);
        }
    }

}

package fr.sanctumcraft.altar;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Chest-style menu made of icons; clicking an icon runs an action. Nothing can be taken out. */
public class ButtonMenu extends GenericContainerScreenHandler {

    private final int rows;
    private final String menuId;
    private final Map<Integer, Consumer<ServerPlayerEntity>> actions;
    private final Map<Integer, Consumer<ServerPlayerEntity>> rightActions;

    public ButtonMenu(int syncId, PlayerInventory inv, int rows, SimpleInventory icons,
                      Map<Integer, Consumer<ServerPlayerEntity>> actions, Map<Integer, Consumer<ServerPlayerEntity>> rightActions,
                      String menuId) {
        super(rows == 6 ? ScreenHandlerType.GENERIC_9X6 : ScreenHandlerType.GENERIC_9X3, syncId, inv, icons, rows);
        this.rows = rows;
        this.menuId = menuId;
        this.actions = actions;
        this.rightActions = rightActions;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < rows * 9) {
            Consumer<ServerPlayerEntity> action = button == 1 && rightActions.containsKey(slotIndex)
                    ? rightActions.get(slotIndex) : actions.get(slotIndex);
            if (action != null && actionType == SlotActionType.PICKUP && player instanceof ServerPlayerEntity sp) {
                syncState();
                action.accept(sp);
                return;
            }
            syncState();
            return;
        }
        if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.PICKUP_ALL) {
            syncState();
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    /** True if this screen is the same menu (same id and size), so it can be refreshed in place. */
    public boolean isSameMenu(String id, int rowCount) {
        return menuId.equals(id) && rows == rowCount;
    }

    /** Refreshes icons and actions without reopening the screen (the mouse doesn't jump). */
    public void apply(Builder b) {
        for (int i = 0; i < rows * 9; i++) getInventory().setStack(i, b.icons.getStack(i));
        actions.clear();
        actions.putAll(b.actions);
        rightActions.clear();
        rightActions.putAll(b.rightActions);
        sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    // ------------------------------------------------------------------ builder

    public static final class Builder {
        final int rows;
        final SimpleInventory icons;
        final Map<Integer, Consumer<ServerPlayerEntity>> actions = new HashMap<>();
        final Map<Integer, Consumer<ServerPlayerEntity>> rightActions = new HashMap<>();

        public Builder(int rows) {
            this.rows = rows;
            this.icons = new SimpleInventory(rows * 9);
            ItemStack filler = icon(Items.BLACK_STAINED_GLASS_PANE, Text.literal(" "), List.of());
            for (int i = 0; i < rows * 9; i++) icons.setStack(i, filler.copy());
        }

        public Builder set(int slot, ItemStack icon, Consumer<ServerPlayerEntity> action) {
            icons.setStack(slot, icon);
            if (action != null) actions.put(slot, action);
            return this;
        }

        /** Left-click and right-click actions. */
        public Builder set(int slot, ItemStack icon, Consumer<ServerPlayerEntity> left, Consumer<ServerPlayerEntity> right) {
            set(slot, icon, left);
            if (right != null) rightActions.put(slot, right);
            return this;
        }

        public ButtonMenu build(int syncId, PlayerInventory inv, String id) {
            return new ButtonMenu(syncId, inv, rows, icons, new HashMap<>(actions), new HashMap<>(rightActions), id);
        }
    }

    public static ItemStack icon(Item item, Text name, List<Text> lore) {
        return icon(new ItemStack(item), name, lore);
    }

    public static ItemStack icon(ItemStack stack, Text name, List<Text> lore) {
        stack.set(DataComponentTypes.CUSTOM_NAME, name.copy().styled(s -> s.withItalic(false)));
        if (!lore.isEmpty()) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore.stream()
                    .map(t -> (Text) t.copy().styled(s -> s.withItalic(false))).toList()));
        }
        return stack;
    }
}

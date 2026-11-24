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
    private final Map<Integer, Consumer<ServerPlayerEntity>> actions;

    public ButtonMenu(int syncId, PlayerInventory inv, int rows, SimpleInventory icons,
                      Map<Integer, Consumer<ServerPlayerEntity>> actions) {
        super(rows == 6 ? ScreenHandlerType.GENERIC_9X6 : ScreenHandlerType.GENERIC_9X3, syncId, inv, icons, rows);
        this.rows = rows;
        this.actions = actions;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < rows * 9) {
            Consumer<ServerPlayerEntity> action = actions.get(slotIndex);
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

        public ButtonMenu build(int syncId, PlayerInventory inv) {
            return new ButtonMenu(syncId, inv, rows, icons, actions);
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

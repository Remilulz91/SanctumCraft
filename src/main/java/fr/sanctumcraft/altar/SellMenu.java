package fr.sanctumcraft.altar;

import fr.sanctumcraft.profile.Profiles;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Sell screen: drop items in the 45 top slots, the bottom row shows the total and a confirm button.
 * Unsellable items (eggs included) stay and are given back when the screen closes.
 */
public class SellMenu extends GenericContainerScreenHandler {

    private static final int SELL_SLOTS = 45;
    private static final int BACK = 45;
    private static final int CONFIRM = 49;

    private final SimpleInventory inv;
    private final ServerPlayerEntity owner;
    private long lastTotal = -1;

    public SellMenu(int syncId, PlayerInventory playerInv, ServerPlayerEntity owner) {
        this(syncId, playerInv, owner, new SimpleInventory(54));
    }

    private SellMenu(int syncId, PlayerInventory playerInv, ServerPlayerEntity owner, SimpleInventory inv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6);
        this.inv = inv;
        this.owner = owner;
        ItemStack filler = ButtonMenu.icon(Items.BLACK_STAINED_GLASS_PANE, Text.literal(" "), List.of());
        for (int i = SELL_SLOTS; i < 54; i++) inv.setStack(i, filler.copy());
        inv.setStack(BACK, ButtonMenu.icon(Items.ARROW, Text.translatable("menu.sanctumcraft.back").formatted(Formatting.GRAY), List.of()));
        refreshTotal();
    }

    private long total() {
        long t = 0;
        for (int i = 0; i < SELL_SLOTS; i++) t += SellPrices.value(inv.getStack(i));
        return t;
    }

    private void refreshTotal() {
        long t = total();
        if (t == lastTotal) return;
        lastTotal = t;
        inv.setStack(CONFIRM, ButtonMenu.icon(Items.GOLD_INGOT,
                Text.translatable("menu.sanctumcraft.sell.confirm", t).formatted(Formatting.GOLD),
                List.of(Text.translatable("menu.sanctumcraft.sell.hint").formatted(Formatting.GRAY),
                        Text.translatable("menu.sanctumcraft.sell.no_eggs").formatted(Formatting.DARK_GRAY))));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= SELL_SLOTS && slotIndex < 54) {
            if (actionType == SlotActionType.PICKUP && player instanceof ServerPlayerEntity sp) {
                if (slotIndex == CONFIRM) {
                    sell(sp);
                } else if (slotIndex == BACK) {
                    sp.closeHandledScreen();
                    AltarMenus.openMain(sp);
                    return;
                }
            }
            syncState();
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Shift-click: player inventory <-> sell area only (never into the bottom row).
        ItemStack moved = ItemStack.EMPTY;
        var s = this.slots.get(slot);
        if (s.hasStack()) {
            ItemStack stack = s.getStack();
            moved = stack.copy();
            if (slot < SELL_SLOTS) {
                if (!insertItem(stack, 54, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (slot >= 54) {
                if (!insertItem(stack, 0, SELL_SLOTS, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
            else s.markDirty();
        }
        return moved;
    }

    private void sell(ServerPlayerEntity player) {
        long earned = 0;
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = inv.getStack(i);
            long v = SellPrices.value(stack);
            if (v > 0) {
                earned += v;
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
        if (earned <= 0) {
            player.sendMessage(Text.translatable("menu.sanctumcraft.sell.nothing").formatted(Formatting.GRAY), true);
            return;
        }
        Profiles.addShells(player, earned);
        player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.MASTER, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("menu.sanctumcraft.sell.done", earned, Profiles.of(player).shells)
                .formatted(Formatting.GOLD), false);
    }

    @Override
    public void sendContentUpdates() {
        refreshTotal();
        super.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Give back everything left in the sell area.
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = inv.removeStack(i);
            if (!stack.isEmpty() && !player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
        }
    }
}

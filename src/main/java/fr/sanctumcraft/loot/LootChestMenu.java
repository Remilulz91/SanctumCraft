package fr.sanctumcraft.loot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** 27-slot chest screen. Hidden slots (placeholders) can't be clicked; items appear one by one. */
public class LootChestMenu extends GenericContainerScreenHandler {

    private final LootChest chest;
    private final ServerPlayerEntity viewer;
    private final BlockPos pos;

    public LootChestMenu(int syncId, PlayerInventory playerInventory, LootChest chest, ServerPlayerEntity viewer, BlockPos pos) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, chest.inventory, 3);
        this.chest = chest;
        this.viewer = viewer;
        this.pos = pos;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 27 && LootChest.isPlaceholder(chest.inventory.getStack(slotIndex))) {
            syncState();
            return;
        }
        if (actionType == SlotActionType.PICKUP_ALL) {
            // Double-click collect could scan hidden slots: keep it simple, refuse it here.
            syncState();
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public void sendContentUpdates() {
        if (viewer.getWorld() instanceof ServerWorld world) {
            LootHandler.tickReveal(world, chest, pos, viewer);
        }
        super.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.squaredDistanceTo(pos.toCenterPos()) <= 64
                && player.getWorld().getBlockState(pos).getBlock() instanceof SanctumChestBlock;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        // GenericContainerScreenHandler.onClosed would call inventory.onClose: nothing to do for us.
        super.onClosed(player);
    }
}

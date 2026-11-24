package fr.sanctumcraft.altar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Sanctum Altar: shop, selling and Hunter profile. Craftable, placed anywhere. */
public class SanctumAltarBlock extends Block {

    public SanctumAltarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            AltarMenus.openMain(sp);
        }
        return ActionResult.SUCCESS;
    }
}

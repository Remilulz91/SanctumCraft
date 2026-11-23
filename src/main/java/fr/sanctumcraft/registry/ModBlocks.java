package fr.sanctumcraft.registry;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.portal.SanctumPortalBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {

    /** Frame block of the sanctum portal. */
    public static final Block RUNE_STONE = register("rune_stone",
            new Block(AbstractBlock.Settings.create()
                    .strength(3.0F, 9.0F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.DEEPSLATE_BRICKS)
                    .luminance(state -> 3)),
            true);

    /** The swirling portal surface. Unbreakable, drops nothing. */
    public static final SanctumPortalBlock SANCTUM_PORTAL = register("sanctum_portal",
            new SanctumPortalBlock(AbstractBlock.Settings.create()
                    .noCollision()
                    .strength(-1.0F, 3600000.0F)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> 11)
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .dropsNothing()),
            false);

    private ModBlocks() {}

    private static <T extends Block> T register(String name, T block, boolean withItem) {
        Registry.register(Registries.BLOCK, SanctumCraft.id(name), block);
        if (withItem) {
            Registry.register(Registries.ITEM, SanctumCraft.id(name), new BlockItem(block, new Item.Settings()));
        }
        return block;
    }

    /** Forces class loading (static registration). */
    public static void register() {}
}

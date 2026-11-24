package fr.sanctumcraft.registry;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.altar.SanctumAltarBlock;
import fr.sanctumcraft.block.DestinationMechanismBlock;
import fr.sanctumcraft.block.EggNestBlock;
import fr.sanctumcraft.loot.ChestTier;
import fr.sanctumcraft.loot.KeyTier;
import fr.sanctumcraft.loot.LootSpotBlock;
import fr.sanctumcraft.loot.SanctumChestBlock;
import fr.sanctumcraft.loot.VaultDoorBlock;
import fr.sanctumcraft.mob.RiftBlock;
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

    /** Egg nest (sanctum only). */
    public static final EggNestBlock EGG_NEST = register("egg_nest",
            new EggNestBlock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .sounds(BlockSoundGroup.ROOTED_DIRT)
                    .nonOpaque()
                    .luminance(state -> state.get(EggNestBlock.ACTIVE) ? 6 : 0)
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .dropsNothing()),
            false);

    /** Destination mechanism (sanctum only). */
    public static final DestinationMechanismBlock DESTINATION_MECHANISM = register("destination_mechanism",
            new DestinationMechanismBlock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .sounds(BlockSoundGroup.LODESTONE)
                    .luminance(state -> state.get(DestinationMechanismBlock.ACTIVE) ? 15 : 5)
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .dropsNothing()),
            false);

    /** Template marker: becomes a sanctum chest when a room is placed. */
    public static final LootSpotBlock LOOT_SPOT = register("loot_spot",
            new LootSpotBlock(AbstractBlock.Settings.create().strength(-1.0F, 3600000.0F).dropsNothing().nonOpaque()),
            true);

    public static final SanctumChestBlock CHEST_BASIC = chest(ChestTier.BASIC);
    public static final SanctumChestBlock CHEST_FINE = chest(ChestTier.FINE);
    public static final SanctumChestBlock CHEST_RARE = chest(ChestTier.RARE);
    public static final SanctumChestBlock CHEST_EPIC = chest(ChestTier.EPIC);
    public static final SanctumChestBlock CHEST_LEGENDARY = chest(ChestTier.LEGENDARY);

    public static final VaultDoorBlock VAULT_DOOR_RUSTED = vaultDoor(KeyTier.RUSTED);
    public static final VaultDoorBlock VAULT_DOOR_SILVER = vaultDoor(KeyTier.SILVER);
    public static final VaultDoorBlock VAULT_DOOR_GILDED = vaultDoor(KeyTier.GILDED);

    /** Sanctum Altar: shop, sell, profile. */
    public static final SanctumAltarBlock ALTAR = register("sanctum_altar",
            new SanctumAltarBlock(AbstractBlock.Settings.create()
                    .strength(3.5F, 9.0F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.LODESTONE)
                    .luminance(state -> 7)),
            true);

    /** Rift (sanctum only): triggers monster waves, then becomes a chest. */
    public static final RiftBlock RIFT = register("rift",
            new RiftBlock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                    .luminance(state -> state.get(RiftBlock.ACTIVE) ? 15 : 9)
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .dropsNothing()),
            false);

    private ModBlocks() {}

    private static SanctumChestBlock chest(ChestTier tier) {
        return register("sanctum_chest_" + tier.id(), new SanctumChestBlock(tier, AbstractBlock.Settings.create()
                .strength(-1.0F, 3600000.0F)
                .sounds(BlockSoundGroup.WOOD)
                .nonOpaque()
                .luminance(state -> tier.ordinal() >= ChestTier.EPIC.ordinal() ? 7 : 0)
                .pistonBehavior(PistonBehavior.BLOCK)
                .dropsNothing()), false);
    }

    private static VaultDoorBlock vaultDoor(KeyTier tier) {
        return register("vault_door_" + tier.name().toLowerCase(), new VaultDoorBlock(tier, AbstractBlock.Settings.create()
                .strength(-1.0F, 3600000.0F)
                .sounds(BlockSoundGroup.METAL)
                .pistonBehavior(PistonBehavior.BLOCK)
                .dropsNothing()), false);
    }

    public static SanctumChestBlock chestFor(ChestTier tier) {
        return switch (tier) {
            case BASIC -> CHEST_BASIC;
            case FINE -> CHEST_FINE;
            case RARE -> CHEST_RARE;
            case EPIC -> CHEST_EPIC;
            case LEGENDARY -> CHEST_LEGENDARY;
        };
    }

    public static VaultDoorBlock vaultDoorFor(KeyTier tier) {
        return switch (tier) {
            case RUSTED -> VAULT_DOOR_RUSTED;
            case SILVER -> VAULT_DOOR_SILVER;
            case GILDED -> VAULT_DOOR_GILDED;
        };
    }

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

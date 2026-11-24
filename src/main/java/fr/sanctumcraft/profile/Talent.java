package fr.sanctumcraft.profile;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

/** Hunter talents: 3 branches x 4 talents x 3 ranks. Effects only apply inside the sanctum. */
public enum Talent {
    // Survival
    VITALITY(Branch.SURVIVAL, Items.GLISTERING_MELON_SLICE),   // +2 hearts / rank
    TOUGHNESS(Branch.SURVIVAL, Items.IRON_CHESTPLATE),         // +2 armor / rank
    FLEET(Branch.SURVIVAL, Items.FEATHER),                     // +5 % speed / rank
    SECOND_WIND(Branch.SURVIVAL, Items.GOLDEN_APPLE),          // regeneration when low health
    // Exploration
    QUICK_HANDS(Branch.EXPLORATION, Items.CHEST),              // -15 % chest search time / rank
    DIGGER(Branch.EXPLORATION, Items.IRON_SHOVEL),             // -2 nest hits / rank
    STRONG_BACK(Branch.EXPLORATION, Items.TURTLE_SCUTE),       // -1 egg slowness level / rank
    NIGHT_EYES(Branch.EXPLORATION, Items.SPIDER_EYE),          // night vision (rank 1+), longer reach of companions' tracking
    // Loot
    GREED(Branch.LOOT, Items.SUNFLOWER),                       // +10 % shells / rank
    KEYSMITH(Branch.LOOT, Items.TRIPWIRE_HOOK),                // +5 % key chance / rank
    TREASURE(Branch.LOOT, Items.EMERALD),                      // 10 % / rank: one extra loot roll
    COLLECTOR(Branch.LOOT, Items.ITEM_FRAME);                  // +3 % collector relic chance / rank

    public static final int MAX_RANK = 3;

    public enum Branch {
        SURVIVAL(Formatting.RED, Items.RED_DYE),
        EXPLORATION(Formatting.AQUA, Items.COMPASS),
        LOOT(Formatting.GOLD, Items.GOLD_INGOT);

        public final Formatting color;
        public final Item icon;

        Branch(Formatting color, Item icon) {
            this.color = color;
            this.icon = icon;
        }

        public String key() {
            return "talent.sanctumcraft.branch." + name().toLowerCase();
        }
    }

    public final Branch branch;
    public final Item icon;

    Talent(Branch branch, Item icon) {
        this.branch = branch;
        this.icon = icon;
    }

    public String key() {
        return "talent.sanctumcraft." + name().toLowerCase();
    }
}

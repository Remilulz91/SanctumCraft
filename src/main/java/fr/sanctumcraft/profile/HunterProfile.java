package fr.sanctumcraft.profile;

import net.minecraft.nbt.NbtCompound;

/** Per-player progression, saved with the world. */
public final class HunterProfile {

    public long shells;
    public int level = 1;
    public int xp;
    public int raids;
    public int extractions;
    public int eggs;
    /** Chaos tickets bought on {@link #chaosDay} (price grows with each purchase). */
    public int chaosBought;
    public String chaosDay = "";

    /** Hunter XP needed to go from {@code level} to {@code level + 1}. */
    public static int xpForNext(int level) {
        return 100 + 50 * (level - 1);
    }

    public NbtCompound toNbt() {
        NbtCompound t = new NbtCompound();
        t.putLong("shells", shells);
        t.putInt("level", level);
        t.putInt("xp", xp);
        t.putInt("raids", raids);
        t.putInt("extractions", extractions);
        t.putInt("eggs", eggs);
        t.putInt("chaosBought", chaosBought);
        t.putString("chaosDay", chaosDay);
        return t;
    }

    public static HunterProfile fromNbt(NbtCompound t) {
        HunterProfile p = new HunterProfile();
        p.shells = t.getLong("shells");
        p.level = Math.max(1, t.getInt("level"));
        p.xp = t.getInt("xp");
        p.raids = t.getInt("raids");
        p.extractions = t.getInt("extractions");
        p.eggs = t.getInt("eggs");
        p.chaosBought = t.getInt("chaosBought");
        p.chaosDay = t.getString("chaosDay");
        return p;
    }
}

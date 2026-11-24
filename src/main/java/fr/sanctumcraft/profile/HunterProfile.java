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
    /** Talent ranks (name -> rank). */
    public final java.util.Map<String, Integer> talents = new java.util.HashMap<>();
    /** Collector relics placed in the showcase (relic names). */
    public final java.util.Set<String> showcase = new java.util.HashSet<>();
    /** Classic relics placed in the showcase (relic names). */
    public final java.util.Set<String> showcaseClassic = new java.util.HashSet<>();
    /** Companion collection and the active one. */
    public final java.util.List<fr.sanctumcraft.companion.CompanionData> companions = new java.util.ArrayList<>();
    public java.util.UUID activeCompanion;
    public boolean whistleGiven;

    public int rank(Talent t) {
        return talents.getOrDefault(t.name(), 0);
    }

    public int spentPoints() {
        int n = 0;
        for (int v : talents.values()) n += v;
        return n;
    }

    /** One talent point per level above 1. */
    public int freePoints() {
        return Math.max(0, (level - 1) - spentPoints());
    }

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
        net.minecraft.nbt.NbtCompound tal = new net.minecraft.nbt.NbtCompound();
        talents.forEach(tal::putInt);
        t.put("talents", tal);
        net.minecraft.nbt.NbtList show = new net.minecraft.nbt.NbtList();
        for (String r : showcase) show.add(net.minecraft.nbt.NbtString.of(r));
        t.put("showcase", show);
        net.minecraft.nbt.NbtList classic = new net.minecraft.nbt.NbtList();
        for (String r : showcaseClassic) classic.add(net.minecraft.nbt.NbtString.of(r));
        t.put("showcaseClassic", classic);
        net.minecraft.nbt.NbtList comps = new net.minecraft.nbt.NbtList();
        for (fr.sanctumcraft.companion.CompanionData c : companions) comps.add(c.toNbt());
        t.put("companions", comps);
        if (activeCompanion != null) t.putUuid("activeCompanion", activeCompanion);
        t.putBoolean("whistleGiven", whistleGiven);
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
        net.minecraft.nbt.NbtCompound tal = t.getCompound("talents");
        for (String k : tal.getKeys()) p.talents.put(k, tal.getInt(k));
        net.minecraft.nbt.NbtList show = t.getList("showcase", net.minecraft.nbt.NbtElement.STRING_TYPE);
        for (int i = 0; i < show.size(); i++) p.showcase.add(show.getString(i));
        net.minecraft.nbt.NbtList classic = t.getList("showcaseClassic", net.minecraft.nbt.NbtElement.STRING_TYPE);
        for (int i = 0; i < classic.size(); i++) p.showcaseClassic.add(classic.getString(i));
        // 0.6.0 placed collector relics directly: they count as classic too.
        p.showcaseClassic.addAll(p.showcase);
        net.minecraft.nbt.NbtList comps = t.getList("companions", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < comps.size(); i++) p.companions.add(fr.sanctumcraft.companion.CompanionData.fromNbt(comps.getCompound(i)));
        if (t.containsUuid("activeCompanion")) p.activeCompanion = t.getUuid("activeCompanion");
        p.whistleGiven = t.getBoolean("whistleGiven");
        return p;
    }
}

package fr.sanctumcraft.guide;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.profile.HunterProfile;
import fr.sanctumcraft.profile.Profiles;
import fr.sanctumcraft.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Sanctum Guide: a written book built from /sanctumcraft/guide.txt (French and English parts,
 * clickable contents). Given on first join, again with /sanctum guide, refreshed when the mod is updated.
 */
public final class SanctumGuide {

    private static final Pattern LINK = Pattern.compile("\\[\\[([^|\\]]+)\\|([^\\]]+)]]");
    private static final String VERSION_KEY = "sanctumcraft_guide";
    private static WrittenBookContentComponent content;
    private static int hash;

    private SanctumGuide() {}

    private record Page(String anchor, List<String> lines) {}

    public static synchronized WrittenBookContentComponent content() {
        if (content == null) build();
        return content;
    }

    private static void build() {
        List<Page> pages = new ArrayList<>();
        StringBuilder all = new StringBuilder();
        try (InputStream in = SanctumGuide.class.getResourceAsStream("/sanctumcraft/guide.txt")) {
            if (in == null) throw new IllegalStateException("guide.txt missing");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                all.append(line).append('\n');
                if (line.startsWith("#")) continue;
                if (line.startsWith("===")) pages.add(new Page(line.substring(3).trim(), new ArrayList<>()));
                else if (!pages.isEmpty()) pages.get(pages.size() - 1).lines().add(line);
            }
        } catch (Exception e) {
            SanctumCraft.LOGGER.error("Could not read the guide", e);
            pages.add(new Page("", List.of("SanctumCraft")));
        }
        hash = all.toString().hashCode();

        Map<String, Integer> anchors = new HashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            if (!pages.get(i).anchor().isEmpty()) anchors.put(pages.get(i).anchor(), i + 1);
        }
        List<RawFilteredPair<Text>> out = new ArrayList<>();
        for (Page page : pages) {
            List<String> lines = new ArrayList<>(page.lines());
            while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) lines.remove(lines.size() - 1);
            String a = page.anchor();
            if (a.startsWith("fr_") && !a.equals("fr_toc")) lines.add("[[« Sommaire|fr_toc]]");
            if (a.startsWith("en_") && !a.equals("en_toc")) lines.add("[[« Contents|en_toc]]");
            MutableText text = Text.empty();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) text.append("\n");
                appendLine(text, lines.get(i), anchors);
            }
            out.add(RawFilteredPair.of(text));
        }
        content = new WrittenBookContentComponent(RawFilteredPair.of("Guide du Sanctum"), "Remilulz_91", 0, out, true);
    }

    private static void appendLine(MutableText text, String line, Map<String, Integer> anchors) {
        Matcher m = LINK.matcher(line);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) text.append(Text.literal(line.substring(last, m.start())));
            Integer page = anchors.get(m.group(2));
            if (page == null) {
                text.append(Text.literal(m.group(1)));
            } else {
                boolean en = m.group(2).startsWith("en_");
                Text hover = Text.literal((en ? "Go to page " : "Aller à la page ") + page);
                text.append(Text.literal(m.group(1)).setStyle(Style.EMPTY
                        .withColor(Formatting.DARK_BLUE).withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(page)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
            }
            last = m.end();
        }
        if (last < line.length()) text.append(Text.literal(line.substring(last)));
    }

    /** A fresh guide book. */
    public static ItemStack create() {
        ItemStack stack = new ItemStack(ModItems.SANCTUM_GUIDE);
        refresh(stack);
        return stack;
    }

    /** Puts the current guide text in the stack. Returns true if it changed. */
    public static boolean refresh(ItemStack stack) {
        WrittenBookContentComponent c = content();
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT) && data != null
                && data.copyNbt().getInt(VERSION_KEY) == hash) return false;
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, c);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(VERSION_KEY, hash));
        return true;
    }

    public static void give(ServerPlayerEntity player) {
        ItemStack book = create();
        if (!player.getInventory().insertStack(book)) player.dropItem(book, false);
        player.sendMessage(Text.translatable("message.sanctumcraft.guide.given").formatted(Formatting.LIGHT_PURPLE), false);
    }

    /** First join: give the guide once. Every join: update guides in the inventory to the current text. */
    public static void onJoin(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(ModItems.SANCTUM_GUIDE)) refresh(s);
        }
        HunterProfile p = Profiles.of(player);
        if (!p.guideGiven) {
            p.guideGiven = true;
            Profiles.dirty(player);
            give(player);
        }
    }
}

package fr.sanctumcraft.update;

import fr.sanctumcraft.SanctumCraft;
import fr.sanctumcraft.config.SanctumConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks for a newer release on GitHub (Remilulz91/SanctumCraft), once, in the background.
 * Pre-releases count too (alpha / beta builds). Never blocks the game; silently gives up when offline.
 */
public final class UpdateChecker {

    public static final String REPO = "Remilulz91/SanctumCraft";
    public static final String RELEASES_URL = "https://github.com/" + REPO + "/releases";
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    /** Newer version found (without the leading v), or null. */
    public static volatile String latest;
    public static volatile boolean done;

    private UpdateChecker() {}

    public static void start() {
        if (!SanctumConfig.updateCheck) {
            done = true;
            return;
        }
        Thread t = new Thread(UpdateChecker::check, "SanctumCraft update check");
        t.setDaemon(true);
        t.start();
    }

    private static void check() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPO + "/releases?per_page=20"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SanctumCraft/" + SanctumCraft.VERSION)
                    .timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return;
            String current = clean(SanctumCraft.VERSION);
            String best = null;
            Matcher m = TAG.matcher(res.body());
            while (m.find()) {
                String v = clean(m.group(1));
                if (best == null || compare(v, best) > 0) best = v;
            }
            if (best != null && compare(best, current) > 0) {
                latest = best;
                SanctumCraft.LOGGER.info("A newer SanctumCraft is available: {} (installed: {}) - {}", best, current, RELEASES_URL);
            }
        } catch (Exception e) {
            SanctumCraft.LOGGER.info("Update check failed: {}", e.toString());
        } finally {
            done = true;
        }
    }

    static String clean(String v) {
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        if (v.endsWith("-debug")) v = v.substring(0, v.length() - 6);
        int plus = v.indexOf('+');
        return plus >= 0 ? v.substring(0, plus) : v;
    }

    /** Semantic versioning order: 0.7.0-alpha.2 < 0.7.0-beta.1 < 0.7.0 < 0.7.1. */
    public static int compare(String a, String b) {
        String[] pa = a.split("-", 2), pb = b.split("-", 2);
        int c = compareIds(pa[0].split("\\."), pb[0].split("\\."));
        if (c != 0) return c;
        if (pa.length == 1 && pb.length == 1) return 0;
        if (pa.length == 1) return 1;   // release > pre-release
        if (pb.length == 1) return -1;
        return compareIds(pa[1].split("\\."), pb[1].split("\\."));
    }

    private static int compareIds(String[] a, String[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            if (i >= a.length) return -1;
            if (i >= b.length) return 1;
            boolean na = a[i].matches("\\d+"), nb = b[i].matches("\\d+");
            int c;
            if (na && nb) c = Long.compare(Long.parseLong(a[i]), Long.parseLong(b[i]));
            else if (na) c = -1;
            else if (nb) c = 1;
            else c = a[i].compareTo(b[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    static List<String> selfTest() {
        List<String> errors = new ArrayList<>();
        String[] order = {"0.6.1-alpha.5", "0.7.0-alpha.1", "0.7.0-alpha.2", "0.7.0-alpha.10", "0.7.0-beta.1", "0.7.0", "0.7.1", "1.0.0"};
        for (int i = 0; i + 1 < order.length; i++) {
            if (compare(order[i], order[i + 1]) >= 0) errors.add(order[i] + " !< " + order[i + 1]);
        }
        return errors;
    }
}

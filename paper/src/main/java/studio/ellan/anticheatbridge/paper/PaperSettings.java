package studio.ellan.anticheatbridge.paper;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record PaperSettings(
    String serverName,
    String permission,
    boolean displayLocally,
    boolean forwardToProxy,
    double minimumViolationLevel,
    Set<String> ignoredChecks,
    boolean vulcan,
    boolean vulcanPunishments,
    boolean grim,
    boolean minerTrack,
    boolean cancelMinerTrackLocal,
    long dedupeMillis
) {
    public static PaperSettings load(FileConfiguration config) {
        Set<String> ignored = new HashSet<>();
        for (String check : config.getStringList("filters.ignored-checks")) {
            ignored.add(check.toLowerCase(Locale.ROOT));
        }

        return new PaperSettings(
            config.getString("server-name", "auto"),
            config.getString("permission", "ellan.anticheat.notify"),
            config.getBoolean("display-locally", false),
            config.getBoolean("forward-to-proxy", true),
            Math.max(0.0, config.getDouble("filters.minimum-vl", 1.0)),
            Set.copyOf(ignored),
            config.getBoolean("sources.vulcan", true),
            config.getBoolean("sources.vulcan-punishments", true),
            config.getBoolean("sources.grim", true),
            config.getBoolean("sources.minertrack", true),
            config.getBoolean("minertrack.cancel-local-message", true),
            Math.max(0L, config.getLong("network.dedupe-millis", 350L))
        );
    }
}

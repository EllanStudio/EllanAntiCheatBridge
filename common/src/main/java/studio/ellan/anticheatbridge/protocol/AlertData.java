package studio.ellan.anticheatbridge.protocol;

import java.util.Objects;
import java.util.UUID;

public record AlertData(
    UUID id,
    long timestamp,
    String server,
    String source,
    String action,
    String player,
    UUID playerId,
    String check,
    String type,
    String description,
    String verbose,
    double violations,
    double maxViolations,
    boolean experimental
) {
    public AlertData {
        id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        timestamp = timestamp <= 0 ? System.currentTimeMillis() : timestamp;
        server = safe(server, "unknown");
        source = safe(source, "unknown");
        action = safe(action, "FLAG");
        player = safe(player, "unknown");
        check = safe(check, "unknown");
        type = safe(type, "");
        description = safe(description, "");
        verbose = safe(verbose, "");
    }

    public AlertData withId(UUID newId) {
        return new AlertData(newId, timestamp, server, source, action, player, playerId,
            check, type, description, verbose, violations, maxViolations, experimental);
    }

    public AlertData withServer(String newServer) {
        return new AlertData(id, timestamp, newServer, source, action, player, playerId,
            check, type, description, verbose, violations, maxViolations, experimental);
    }

    public AlertData withAction(String newAction) {
        return new AlertData(id, timestamp, server, source, newAction, player, playerId,
            check, type, description, verbose, violations, maxViolations, experimental);
    }

    private static String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

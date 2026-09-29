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
    double tps,
    int ping,
    String clientBrand,
    String clientVersion,
    String antiCheatVersion,
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
        clientBrand = safe(clientBrand, "");
        clientVersion = safe(clientVersion, "");
        antiCheatVersion = safe(antiCheatVersion, "");
    }

    public AlertData(
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
        this(id, timestamp, server, source, action, player, playerId, check, type,
            description, verbose, violations, maxViolations, 20.0, 0, "", "", "",
            experimental);
    }

    public AlertData withId(UUID newId) {
        return new AlertData(newId, timestamp, server, source, action, player, playerId,
            check, type, description, verbose, violations, maxViolations, tps, ping,
            clientBrand, clientVersion, antiCheatVersion, experimental);
    }

    public AlertData withServer(String newServer) {
        return new AlertData(id, timestamp, newServer, source, action, player, playerId,
            check, type, description, verbose, violations, maxViolations, tps, ping,
            clientBrand, clientVersion, antiCheatVersion, experimental);
    }

    public AlertData withAction(String newAction) {
        return new AlertData(id, timestamp, server, source, newAction, player, playerId,
            check, type, description, verbose, violations, maxViolations, tps, ping,
            clientBrand, clientVersion, antiCheatVersion, experimental);
    }

    private static String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

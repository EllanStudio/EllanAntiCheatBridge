package studio.ellan.anticheatbridge.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

record VelocitySettings(
    boolean logConsole,
    int maxAlertsPerSecond,
    boolean broadcastToSource
) {
    static VelocitySettings load(Path dataDirectory) {
        Path path = dataDirectory.resolve("config.properties");
        createDefaultIfMissing(dataDirectory, path);
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
                // Keep defaults if the file cannot be read.
            }
        }
        return new VelocitySettings(
            Boolean.parseBoolean(properties.getProperty("log-console", "true")),
            Math.max(1, Integer.parseInt(properties.getProperty("max-alerts-per-second", "80"))),
            Boolean.parseBoolean(properties.getProperty("broadcast-to-source", "true"))
        );
    }

    private static void createDefaultIfMissing(Path dataDirectory, Path path) {
        if (Files.isRegularFile(path)) {
            return;
        }
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(
                path,
                """
                # Log every network alert to the Velocity console.
                log-console=true

                # Maximum alerts accepted from one backend in a rolling second.
                max-alerts-per-second=80

                # Send broadcasts back to the backend that created them.
                broadcast-to-source=true
                """,
                StandardOpenOption.CREATE_NEW
            );
        } catch (IOException ignored) {
            // Defaults are still used when the file cannot be created.
        }
    }
}

package studio.ellan.anticheatbridge.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

record VelocitySettings(
    boolean logConsole,
    int maxAlertsPerSecond,
    boolean broadcastToSource
) {
    static VelocitySettings load(Path dataDirectory) {
        Path path = dataDirectory.resolve("config.properties");
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
}

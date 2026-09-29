package studio.ellan.anticheatbridge.paper;

import org.bukkit.plugin.java.JavaPlugin;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

final class DescriptionTranslator {
    private static final String RESOURCE = "vulcan-descriptions_zh_cn.properties";
    private final Map<String, String> descriptions;

    private DescriptionTranslator(Map<String, String> descriptions) {
        this.descriptions = descriptions;
    }

    static DescriptionTranslator load(JavaPlugin plugin) {
        plugin.saveResource(RESOURCE, false);
        File file = new File(plugin.getDataFolder(), RESOURCE);
        Properties properties = new Properties();
        try (InputStream input = java.nio.file.Files.newInputStream(file.toPath());
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to load check descriptions: " + exception.getMessage());
        }

        Map<String, String> descriptions = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            descriptions.put(normalize(key), properties.getProperty(key));
        }
        return new DescriptionTranslator(Map.copyOf(descriptions));
    }

    String translate(AlertData alert) {
        if (!"Vulcan".equalsIgnoreCase(alert.source())) {
            return alert.description();
        }
        String key = normalize(alert.check() + alert.type());
        return descriptions.getOrDefault(key, alert.description());
    }

    private static String normalize(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}

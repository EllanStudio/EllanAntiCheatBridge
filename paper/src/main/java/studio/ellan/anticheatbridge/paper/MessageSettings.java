package studio.ellan.anticheatbridge.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

record MessageSettings(
    String prefix,
    String line,
    String rawPrefix,
    List<String> hover,
    Map<String, String> actions
) {
    static MessageSettings load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return new MessageSettings(
            config.getString("prefix", "<#68766E>[<#78B7A1><bold>反作弊</bold><#68766E>] "),
            config.getString(
                "line",
                "<prefix><#68766E>[<#E8EEE9><server><#68766E>] <#78B7A1><source> <#E8EEE9><player><#68766E> › "
                    + "<#D9BC7C><check><type_segment> <#68766E>· <action> <#D9BC7C><vl>"
            ),
            config.getString("raw-prefix", "<prefix><#68766E>[<#E8EEE9><server><#68766E>] "),
            config.getStringList("hover"),
            Map.of(
                "flag", config.getString("actions.flag", "触发"),
                "punish", config.getString("actions.punish", "已处罚"),
                "setback", config.getString("actions.setback", "回退")
            )
        );
    }
}

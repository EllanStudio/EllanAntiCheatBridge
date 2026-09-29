package studio.ellan.anticheatbridge.paper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.util.Locale;
import java.util.UUID;

final class MinerTrackHook implements Listener {
    private final EllanAntiCheatPaperPlugin plugin;
    private boolean enabled;

    MinerTrackHook(EllanAntiCheatPaperPlugin plugin) {
        this.plugin = plugin;
    }

    boolean enable() {
        if (plugin.getServer().getPluginManager().getPlugin("MinerTrack") == null) {
            return false;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
        plugin.getLogger().info("MinerTrack command hook enabled.");
        return true;
    }

    boolean isEnabled() {
        return enabled;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = messageFromCommand(event.getMessage());
        if (message == null) {
            return;
        }
        if (plugin.settings().cancelMinerTrackLocal()) {
            event.setCancelled(true);
        }
        forward(message, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onServerCommand(ServerCommandEvent event) {
        String message = messageFromCommand(event.getCommand());
        if (message == null) {
            return;
        }
        if (plugin.settings().cancelMinerTrackLocal()) {
            event.setCancelled(true);
        }
        forward(message, plugin.firstOnlinePlayer());
    }

    private String messageFromCommand(String raw) {
        String command = raw == null ? "" : raw.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String[] parts = command.split("\\s+", 3);
        if (parts.length < 3) {
            return null;
        }

        String root = stripNamespace(parts[0].toLowerCase(Locale.ROOT));
        if (!(root.equals("minertrack") || root.equals("mtrack"))
            || !parts[1].equalsIgnoreCase("notify")) {
            return null;
        }
        String message = parts[2].trim();
        return message.isEmpty() ? null : message;
    }

    private String stripNamespace(String command) {
        int namespace = command.indexOf(':');
        return namespace >= 0 ? command.substring(namespace + 1) : command;
    }

    private void forward(String message, Player transport) {
        AlertData alert = new AlertData(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            plugin.serverName(),
            "MinerTrack",
            "NOTIFY",
            "MinerTrack",
            null,
            "MinerTrack",
            "",
            message,
            "",
            999.0,
            0.0,
            false
        );
        plugin.report(alert, transport);
    }
}

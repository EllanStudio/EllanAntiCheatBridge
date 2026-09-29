package studio.ellan.anticheatbridge.paper;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.util.UUID;

final class VulcanHook {
    private final EllanAntiCheatPaperPlugin plugin;
    private final Listener listener;
    private boolean enabled;

    VulcanHook(EllanAntiCheatPaperPlugin plugin) {
        this.plugin = plugin;
        this.listener = new Listener() {
        };
    }

    boolean enable() {
        Plugin vulcan = plugin.getServer().getPluginManager().getPlugin("Vulcan");
        if (vulcan == null || !vulcan.isEnabled()) {
            return false;
        }

        boolean postFlag = register("me.frep.vulcan.api.event.VulcanPostFlagEvent", false);
        boolean punish = register("me.frep.vulcan.api.event.VulcanPunishEvent", true);
        enabled = postFlag || punish;
        if (enabled) {
            plugin.getLogger().info("Vulcan hook enabled (postFlag=" + postFlag + ", punish=" + punish + ").");
        }
        return enabled;
    }

    boolean isEnabled() {
        return enabled;
    }

    private boolean register(String className, boolean punishment) {
        Class<?> raw = ReflectionUtil.findClass(className);
        if (raw == null || !Event.class.isAssignableFrom(raw)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Class<? extends Event> eventClass = (Class<? extends Event>) raw;
        EventExecutor executor = (ignored, event) -> handle(event, punishment);
        plugin.getServer().getPluginManager().registerEvent(
            eventClass,
            listener,
            EventPriority.MONITOR,
            executor,
            plugin,
            false
        );
        return true;
    }

    private void handle(Event event, boolean punishment) {
        if (punishment && !plugin.settings().vulcanPunishments()) {
            return;
        }

        Object playerObject = ReflectionUtil.invoke(event, "getPlayer");
        if (!(playerObject instanceof Player player)) {
            return;
        }
        Object check = ReflectionUtil.invoke(event, "getCheck");
        if (check == null) {
            return;
        }

        String checkName = ReflectionUtil.string(check, "getName", "Unknown");
        String checkType = ReflectionUtil.string(check, "getType", "");
        double violations = ReflectionUtil.number(check, "getVl", 0.0);
        double maxViolations = ReflectionUtil.number(check, "getMaxVl", 0.0);

        AlertData alert = new AlertData(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            plugin.serverName(),
            "Vulcan",
            punishment ? "PUNISH" : "FLAG",
            player.getName(),
            player.getUniqueId(),
            checkName,
            checkType,
            ReflectionUtil.string(check, "getDescription", ""),
            ReflectionUtil.string(event, "getInfo", ""),
            violations,
            maxViolations,
            ReflectionUtil.bool(check, "isExperimental", false)
        );
        plugin.report(alert, player);
    }
}

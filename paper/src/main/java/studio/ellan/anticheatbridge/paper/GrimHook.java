package studio.ellan.anticheatbridge.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

final class GrimHook {
    private final EllanAntiCheatPaperPlugin plugin;
    private boolean enabled;
    private boolean registering;

    GrimHook(EllanAntiCheatPaperPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isEnabled() {
        return enabled;
    }

    void enable() {
        if (enabled || registering) {
            return;
        }

        Plugin grim = plugin.getServer().getPluginManager().getPlugin("GrimAC");
        if (grim == null || !grim.isEnabled()) {
            return;
        }
        registering = true;
        tryRegister(grim.getClass().getClassLoader(), 0);
    }

    private void tryRegister(ClassLoader classLoader, int attempt) {
        if (enabled) {
            registering = false;
            return;
        }
        if (attempt >= 40) {
            registering = false;
            plugin.getLogger().warning("Grim hook was not available after startup.");
            return;
        }

        try {
            Class<?> apiClass = Class.forName("ac.grim.grimac.GrimAPI", false, classLoader);
            Object api = ReflectionUtil.staticField(apiClass, "INSTANCE");
            if (api == null) {
                scheduleRetry(classLoader, attempt);
                return;
            }

            Object eventBus = api.getClass().getMethod("getEventBus").invoke(api);
            Class<?> flagClass = Class.forName(
                "ac.grim.grimac.api.event.events.FlagEvent",
                false,
                classLoader
            );
            Class<?> listenerClass = Class.forName(
                "ac.grim.grimac.api.event.GrimEventListener",
                false,
                classLoader
            );
            Object listener = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("handle") && args != null && args.length == 1) {
                        handleFlag(args[0]);
                    }
                    if (method.getName().equals("toString")) {
                        return "EllanGrimListener";
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    return null;
                }
            );

            Method subscribe = eventBus.getClass().getMethod(
                "subscribe",
                Object.class,
                Class.class,
                listenerClass
            );
            subscribe.invoke(eventBus, plugin, flagClass, listener);
            enabled = true;
            registering = false;
            plugin.getLogger().info("Grim hook enabled.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            scheduleRetry(classLoader, attempt);
        }
    }

    private void scheduleRetry(ClassLoader classLoader, int attempt) {
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> tryRegister(classLoader, attempt + 1),
            20L
        );
    }

    private void handleFlag(Object event) {
        Object user = ReflectionUtil.invoke(event, "getUser");
        Object check = ReflectionUtil.invoke(event, "getCheck");
        if (user == null || check == null) {
            return;
        }

        String playerName = ReflectionUtil.string(user, "getName", "unknown");
        Player player = Bukkit.getPlayerExact(playerName);
        String checkName = ReflectionUtil.string(check, "getCheckName", "Unknown");
        double violations = ReflectionUtil.number(check, "getViolations", 0.0);

        AlertData alert = new AlertData(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            plugin.serverName(),
            "Grim",
            "FLAG",
            playerName,
            player == null ? null : player.getUniqueId(),
            checkName,
            ReflectionUtil.string(check, "getAlternativeName", ""),
            ReflectionUtil.string(check, "getDescription", ""),
            ReflectionUtil.string(event, "getVerbose", ""),
            violations,
            0.0,
            Bukkit.getTPS()[0],
            player == null ? 0 : player.getPing(),
            ReflectionUtil.string(user, "getBrand", ""),
            ReflectionUtil.string(user, "getVersionName", ""),
            pluginVersion("GrimAC"),
            ReflectionUtil.bool(check, "isExperimental", false)
        );
        plugin.report(alert, player);
    }

    private String pluginVersion(String pluginName) {
        Plugin installed = plugin.getServer().getPluginManager().getPlugin(pluginName);
        return installed == null ? "" : installed.getDescription().getVersion();
    }
}

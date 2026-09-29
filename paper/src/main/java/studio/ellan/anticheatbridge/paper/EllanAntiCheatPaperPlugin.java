package studio.ellan.anticheatbridge.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import studio.ellan.anticheatbridge.protocol.AlertData;
import studio.ellan.anticheatbridge.protocol.BridgeProtocol;
import studio.ellan.anticheatbridge.protocol.PacketType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EllanAntiCheatPaperPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private final Set<UUID> alertsDisabled = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> recentAlerts = new ConcurrentHashMap<>();
    private PaperSettings settings;
    private MessageSettings messages;
    private AlertRenderer renderer;
    private VulcanHook vulcanHook;
    private GrimHook grimHook;
    private MinerTrackHook minerTrackHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        settings = PaperSettings.load(getConfig());
        messages = MessageSettings.load(this);
        renderer = new AlertRenderer(messages);

        getServer().getMessenger().registerOutgoingPluginChannel(this, BridgeProtocol.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(
            this,
            BridgeProtocol.CHANNEL,
            (channel, player, message) -> receive(message)
        );

        if (getCommand("ellanac") != null) {
            getCommand("ellanac").setExecutor(this);
            getCommand("ellanac").setTabCompleter(this);
        }

        vulcanHook = new VulcanHook(this);
        if (settings.vulcan()) {
            vulcanHook.enable();
        }

        grimHook = new GrimHook(this);
        if (settings.grim()) {
            grimHook.enable();
        }

        minerTrackHook = new MinerTrackHook(this);
        if (settings.minerTrack()) {
            minerTrackHook.enable();
        }

        getLogger().info("Anti-cheat bridge enabled as server '" + serverName() + "'.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    public PaperSettings settings() {
        return settings;
    }

    public String serverName() {
        if (settings != null && !settings.serverName().isBlank()
            && !"auto".equalsIgnoreCase(settings.serverName())) {
            return settings.serverName();
        }
        String configured = System.getProperty("ellan.server.name");
        return configured == null || configured.isBlank() ? "unknown" : configured;
    }

    public void report(AlertData alert, Player transportPlayer) {
        if (!Bukkit.isPrimaryThread()) {
            getServer().getScheduler().runTask(this, () -> report(alert, transportPlayer));
            return;
        }
        PaperSettings current = settings;
        if (current == null || !accept(alert)) {
            return;
        }
        if (current.displayLocally()) {
            display(alert);
        }
        if (current.forwardToProxy()) {
            Player transport = transportPlayer;
            if (transport == null && alert.playerId() != null) {
                transport = Bukkit.getPlayer(alert.playerId());
            }
            sendToProxy(alert, transport);
        }
    }

    private boolean accept(AlertData alert) {
        PaperSettings current = settings;
        if (!"MinerTrack".equalsIgnoreCase(alert.source())
            && alert.violations() < current.minimumViolationLevel()) {
            return false;
        }
        String check = alert.check().toLowerCase(Locale.ROOT);
        if (current.ignoredChecks().contains(check)) {
            return false;
        }

        long now = System.currentTimeMillis();
        String key = alert.source() + "|" + alert.player() + "|" + alert.check() + "|"
            + alert.type() + "|" + alert.action() + "|" + Math.round(alert.violations());
        Long previous = recentAlerts.put(key, now);
        if (previous != null && now - previous < current.dedupeMillis()) {
            return false;
        }
        if (recentAlerts.size() > 2048) {
            recentAlerts.entrySet().removeIf(entry -> now - entry.getValue() > 30_000L);
        }
        return true;
    }

    private void sendToProxy(AlertData alert, Player transport) {
        if (transport == null || !transport.isOnline()) {
            return;
        }
        try {
            transport.sendPluginMessage(this, BridgeProtocol.CHANNEL, BridgeProtocol.encode(PacketType.REPORT, alert));
        } catch (IOException exception) {
            getLogger().warning("Failed to encode alert: " + exception.getMessage());
        }
    }

    private void receive(byte[] message) {
        try {
            BridgeProtocol.Envelope envelope = BridgeProtocol.decode(message);
            if (envelope.type() == PacketType.BROADCAST) {
                display(envelope.alert());
            }
        } catch (IOException exception) {
            getLogger().warning("Received invalid bridge packet: " + exception.getMessage());
        }
    }

    private void display(AlertData alert) {
        PaperSettings current = settings;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (alertsDisabled.contains(online.getUniqueId())
                || !online.hasPermission(current.permission())) {
                continue;
            }
            online.sendMessage(renderer.render(alert));
        }
        getLogger().info("[AntiCheatBridge] " + alert.server() + " " + alert.source() + " "
            + alert.player() + " " + alert.check() + " VL=" + alert.violations());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§8[§6艾尔岚反作弊§8] §7用法: /ellanac <status|alerts|reload|test>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "alerts" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c该命令只能由玩家执行。");
                    return true;
                }
                if (alertsDisabled.remove(player.getUniqueId())) {
                    player.sendMessage("§8[§6艾尔岚反作弊§8] §7跨服告警已§a开启§7。");
                } else {
                    alertsDisabled.add(player.getUniqueId());
                    player.sendMessage("§8[§6艾尔岚反作弊§8] §7跨服告警已§c关闭§7。");
                }
                return true;
            }
            case "status" -> {
                if (!sender.hasPermission("ellan.anticheat.admin")) {
                    sender.sendMessage("§c你没有权限。");
                    return true;
                }
                sender.sendMessage("§8[§6艾尔岚反作弊§8] §7服务器: §f" + serverName());
                sender.sendMessage("§8• §7Vulcan hook: §f" + (vulcanHook != null && vulcanHook.isEnabled()));
                sender.sendMessage("§8• §7Grim hook: §f" + (grimHook != null && grimHook.isEnabled()));
                sender.sendMessage("§8• §7MinerTrack hook: §f" + (minerTrackHook != null && minerTrackHook.isEnabled()));
                sender.sendMessage("§8• §7转发到 Velocity: §f" + settings.forwardToProxy());
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("ellan.anticheat.admin")) {
                    sender.sendMessage("§c你没有权限。");
                    return true;
                }
                reloadConfig();
                settings = PaperSettings.load(getConfig());
                messages = MessageSettings.load(this);
                renderer = new AlertRenderer(messages);
                sender.sendMessage("§8[§6艾尔岚反作弊§8] §a配置已重载。");
                return true;
            }
            case "test" -> {
                if (!sender.hasPermission("ellan.anticheat.admin")) {
                    sender.sendMessage("§c你没有权限。");
                    return true;
                }
                Player player = sender instanceof Player source ? source : firstOnlinePlayer();
                AlertData alert = new AlertData(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    serverName(),
                    "Bridge",
                    "FLAG",
                    player == null ? "Console" : player.getName(),
                    player == null ? null : player.getUniqueId(),
                    "TestCheck",
                    "A",
                    "测试告警",
                    "这是一条测试消息。",
                    3.0,
                    10.0,
                    Bukkit.getTPS()[0],
                    player == null ? 0 : player.getPing(),
                    player == null || player.getClientBrandName() == null
                        ? ""
                        : player.getClientBrandName(),
                    "",
                    "Bridge",
                    false
                );
                display(alert);
                sendToProxy(alert, player);
                sender.sendMessage("§8[§6艾尔岚反作弊§8] §a测试告警已发送。");
                return true;
            }
            default -> {
                sender.sendMessage("§8[§6艾尔岚反作弊§8] §7用法: /ellanac <status|alerts|reload|test>");
                return true;
            }
        }
    }

    Player firstOnlinePlayer() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            return player;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            for (String value : List.of("status", "alerts", "reload", "test")) {
                if (value.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    values.add(value);
                }
            }
            return values;
        }
        return List.of();
    }
}

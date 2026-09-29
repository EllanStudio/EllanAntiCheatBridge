package studio.ellan.anticheatbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import studio.ellan.anticheatbridge.protocol.AlertData;
import studio.ellan.anticheatbridge.protocol.BridgeProtocol;
import studio.ellan.anticheatbridge.protocol.PacketType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(
    id = "ellan-anticheat-bridge",
    name = "EllanAntiCheatBridge",
    version = "1.0.0",
    description = "Cross-server Vulcan and Grim alert bridge.",
    authors = {"EllanStudio"}
)
public final class EllanAntiCheatVelocityPlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final MinecraftChannelIdentifier channel =
        MinecraftChannelIdentifier.create("ellan", "ac_bridge");
    private final AtomicLong received = new AtomicLong();
    private VelocitySettings settings;
    private RateLimiter rateLimiter;

    @Inject
    public EllanAntiCheatVelocityPlugin(
        ProxyServer proxy,
        Logger logger,
        @DataDirectory Path dataDirectory
    ) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        settings = VelocitySettings.load(dataDirectory);
        rateLimiter = new RateLimiter(settings.maxAlertsPerSecond());
        proxy.getChannelRegistrar().register(channel);
        proxy.getCommandManager().register("ellanac", new BridgeCommand(), "eac");
        logger.info("Ellan anti-cheat bridge enabled on {} ({}).",
            proxy.getVersion().getName(), proxy.getVersion().getVersion());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!channel.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String sourceServer = sourceServer(event.getSource());
        if (!rateLimiter.allow(sourceServer)) {
            logger.warn("Dropped anti-cheat alert flood from server '{}'.", sourceServer);
            return;
        }

        try {
            BridgeProtocol.Envelope envelope = BridgeProtocol.decode(event.getData());
            if (envelope.type() != PacketType.REPORT) {
                return;
            }
            broadcast(envelope.alert().withServer(sourceServer));
        } catch (IOException exception) {
            logger.warn("Invalid anti-cheat bridge packet from '{}': {}",
                sourceServer, exception.getMessage());
        }
    }

    private String sourceServer(ChannelMessageSource source) {
        if (source instanceof ServerConnection connection) {
            return connection.getServer().getServerInfo().getName();
        }
        if (source instanceof Player player) {
            return player.getCurrentServer()
                .map(connection -> connection.getServer().getServerInfo().getName())
                .orElse("unknown");
        }
        return "unknown";
    }

    private void broadcast(AlertData alert) {
        received.incrementAndGet();
        try {
            byte[] payload = BridgeProtocol.encode(PacketType.BROADCAST, alert);
            for (RegisteredServer server : proxy.getAllServers()) {
                if (!settings.broadcastToSource()
                    && server.getServerInfo().getName().equalsIgnoreCase(alert.server())) {
                    continue;
                }
                server.sendPluginMessage(channel, payload);
            }
            if (settings.logConsole()) {
                logger.info("[{}] {} {} {} VL={}",
                    alert.server(), alert.source(), alert.player(), alert.check(), alert.violations());
            }
        } catch (IOException exception) {
            logger.warn("Failed to broadcast anti-cheat alert: {}", exception.getMessage());
        }
    }

    private final class BridgeCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (!source.hasPermission("ellan.anticheat.admin")) {
                source.sendPlainMessage("§c你没有权限。");
                return;
            }

            String[] arguments = invocation.arguments();
            if (arguments.length == 0 || arguments[0].equalsIgnoreCase("status")) {
                source.sendPlainMessage("§8[§6艾尔岚反作弊§8] §7Velocity 已接收 §f"
                    + received.get() + " §7条跨服告警。");
                return;
            }
            if (arguments[0].equalsIgnoreCase("reload")) {
                settings = VelocitySettings.load(dataDirectory);
                rateLimiter = new RateLimiter(settings.maxAlertsPerSecond());
                source.sendPlainMessage("§8[§6艾尔岚反作弊§8] §a配置已重载。");
                return;
            }
            if (arguments[0].equalsIgnoreCase("test")) {
                broadcast(new AlertData(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    "velocity",
                    "Bridge",
                    "FLAG",
                    "Console",
                    null,
                    "TestCheck",
                    "A",
                    "Velocity 测试告警",
                    "这是一条跨服测试消息。",
                    3.0,
                    10.0,
                    false
                ));
                source.sendPlainMessage("§8[§6艾尔岚反作弊§8] §a测试告警已广播。");
                return;
            }
            source.sendPlainMessage("§8[§6艾尔岚反作弊§8] §7用法: /ellanac <status|reload|test>");
        }
    }
}

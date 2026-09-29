package studio.ellan.anticheatbridge.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class AlertRenderer {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private AlertRenderer() {
    }

    static Component render(AlertData alert) {
        NamedTextColor sourceColor = switch (alert.source().toLowerCase()) {
            case "vulcan" -> NamedTextColor.GOLD;
            case "grim" -> NamedTextColor.AQUA;
            default -> NamedTextColor.LIGHT_PURPLE;
        };

        String action = switch (alert.action().toUpperCase()) {
            case "PUNISH" -> "已处罚";
            case "SETBACK" -> "回退";
            default -> "触发";
        };

        Component line = Component.text()
            .append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("艾尔岚反作弊", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
            .append(Component.text("[" + alert.server() + "] ", NamedTextColor.GREEN))
            .append(Component.text(alert.source(), sourceColor))
            .append(Component.text(" ", NamedTextColor.GRAY))
            .append(Component.text(alert.player(), NamedTextColor.WHITE))
            .append(Component.text(" › ", NamedTextColor.DARK_GRAY))
            .append(Component.text(alert.check(), NamedTextColor.AQUA))
            .append(alert.type().isBlank()
                ? Component.empty()
                : Component.text(" (" + alert.type() + ")", NamedTextColor.GRAY))
            .append(Component.text(" · " + action + " ", NamedTextColor.GRAY))
            .append(Component.text(formatVl(alert), NamedTextColor.RED))
            .hoverEvent(HoverEvent.showText(hover(alert)))
            .clickEvent(ClickEvent.copyToClipboard(alert.player()))
            .build();
        return line;
    }

    private static Component hover(AlertData alert) {
        Component hover = Component.empty()
            .append(Component.text("来源服务器: ", NamedTextColor.GRAY))
            .append(Component.text(alert.server(), NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("反作弊: ", NamedTextColor.GRAY))
            .append(Component.text(alert.source(), NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("检测: ", NamedTextColor.GRAY))
            .append(Component.text(alert.check() + (alert.type().isBlank() ? "" : " " + alert.type()), NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("时间: ", NamedTextColor.GRAY))
            .append(Component.text(TIME_FORMAT.format(Instant.ofEpochMilli(alert.timestamp())), NamedTextColor.WHITE));

        if (!alert.description().isBlank()) {
            hover = hover
                .append(Component.newline())
                .append(Component.text("说明: ", NamedTextColor.GRAY))
                .append(Component.text(alert.description(), NamedTextColor.WHITE));
        }
        if (!alert.verbose().isBlank()) {
            hover = hover
                .append(Component.newline())
                .append(Component.text("详情: ", NamedTextColor.GRAY))
                .append(Component.text(alert.verbose(), NamedTextColor.WHITE));
        }
        return hover.append(Component.newline())
            .append(Component.text("点击复制玩家名", NamedTextColor.DARK_GRAY))
            ;
    }

    private static String formatVl(AlertData alert) {
        String vl = trim(alert.violations());
        if (alert.maxViolations() > 0) {
            return "VL " + vl + "/" + trim(alert.maxViolations());
        }
        return "VL " + vl;
    }

    private static String trim(double value) {
        return value == Math.rint(value)
            ? Long.toString((long) value)
            : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}

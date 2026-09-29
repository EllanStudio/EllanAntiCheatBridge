package studio.ellan.anticheatbridge.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class AlertRenderer {
    private static final TextColor STRUCTURE = TextColor.fromHexString("#68766E");
    private static final TextColor BRAND = TextColor.fromHexString("#78B7A1");
    private static final TextColor TEXT = TextColor.fromHexString("#E8EEE9");
    private static final TextColor VALUE = TextColor.fromHexString("#D9BC7C");

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private AlertRenderer() {
    }

    static Component render(AlertData alert) {
        if ("MinerTrack".equalsIgnoreCase(alert.source())) {
            return Component.empty()
                .append(Component.text("[", STRUCTURE))
                .append(Component.text("反作弊", BRAND, TextDecoration.BOLD))
                .append(Component.text("] ", STRUCTURE))
                .append(Component.text("[" + alert.server() + "] ", STRUCTURE))
                .append(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(alert.description()));
        }

        String action = switch (alert.action().toUpperCase()) {
            case "PUNISH" -> "已处罚";
            case "SETBACK" -> "回退";
            default -> "触发";
        };

        return Component.empty()
            .append(Component.text("[", STRUCTURE))
            .append(Component.text("反作弊", BRAND, TextDecoration.BOLD))
            .append(Component.text("] ", STRUCTURE))
            .append(Component.text("[" + alert.server() + "] ", STRUCTURE))
            .append(Component.text(alert.source() + " ", BRAND))
            .append(Component.text(alert.player(), TEXT))
            .append(Component.text(" › ", STRUCTURE))
            .append(Component.text(alert.check(), VALUE))
            .append(alert.type().isBlank()
                ? Component.empty()
                : Component.text(" (" + alert.type() + ")", STRUCTURE))
            .append(Component.text(" · " + action + " ", STRUCTURE))
            .append(Component.text(formatVl(alert), VALUE))
            .hoverEvent(HoverEvent.showText(hover(alert)))
            .clickEvent(ClickEvent.copyToClipboard(alert.player()));
    }

    private static Component hover(AlertData alert) {
        Component hover = Component.empty()
            .append(Component.text("来源服务器: ", STRUCTURE))
            .append(Component.text(alert.server(), TEXT))
            .append(Component.newline())
            .append(Component.text("反作弊: ", STRUCTURE))
            .append(Component.text(alert.source(), BRAND))
            .append(Component.newline())
            .append(Component.text("检测: ", STRUCTURE))
            .append(Component.text(alert.check(), VALUE))
            .append(alert.type().isBlank()
                ? Component.empty()
                : Component.text(" " + alert.type(), TEXT))
            .append(Component.newline())
            .append(Component.text("时间: ", STRUCTURE))
            .append(Component.text(TIME_FORMAT.format(Instant.ofEpochMilli(alert.timestamp())), TEXT));

        if (!alert.description().isBlank()) {
            hover = hover
                .append(Component.newline())
                .append(Component.text("说明: ", STRUCTURE))
                .append(Component.text(alert.description(), TEXT));
        }
        if (!alert.verbose().isBlank()) {
            hover = hover
                .append(Component.newline())
                .append(Component.text("详情: ", STRUCTURE))
                .append(Component.text(alert.verbose(), TEXT));
        }
        return hover.append(Component.newline())
            .append(Component.text("点击复制玩家名", STRUCTURE));
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

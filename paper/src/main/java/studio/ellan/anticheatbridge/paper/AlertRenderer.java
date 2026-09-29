package studio.ellan.anticheatbridge.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import studio.ellan.anticheatbridge.protocol.AlertData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class AlertRenderer {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final MessageSettings settings;
    private final DescriptionTranslator descriptions;

    AlertRenderer(MessageSettings settings, DescriptionTranslator descriptions) {
        this.settings = settings;
        this.descriptions = descriptions;
    }

    Component render(AlertData alert) {
        TagResolver resolver = resolver(alert);
        if ("MinerTrack".equalsIgnoreCase(alert.source())) {
            return MiniMessage.miniMessage()
                .deserialize(settings.rawPrefix(), resolver)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(alert.description()))
                .hoverEvent(HoverEvent.showText(hover(alert, resolver)))
                .clickEvent(ClickEvent.copyToClipboard(alert.player()));
        }

        return MiniMessage.miniMessage()
            .deserialize(settings.line(), resolver)
            .hoverEvent(HoverEvent.showText(hover(alert, resolver)))
            .clickEvent(ClickEvent.copyToClipboard(alert.player()));
    }

    private Component hover(AlertData alert, TagResolver resolver) {
        Component result = Component.empty();
        boolean first = true;
        for (String template : settings.hover()) {
            if ((template.contains("<description>") && alert.description().isBlank())
                || (template.contains("<verbose>") && alert.verbose().isBlank())) {
                continue;
            }
            if (!first) {
                result = result.append(Component.newline());
            }
            result = result.append(MiniMessage.miniMessage().deserialize(template, resolver));
            first = false;
        }
        return result;
    }

    private TagResolver resolver(AlertData alert) {
        String typeSegment = alert.type().isBlank()
            ? ""
            : " <#68766E>(<#E8EEE9>" + alert.type() + "<#68766E>)";

        return TagResolver.resolver(
            Placeholder.parsed("prefix", settings.prefix()),
            Placeholder.unparsed("server", alert.server()),
            Placeholder.unparsed("source", alert.source()),
            Placeholder.unparsed("player", alert.player()),
            Placeholder.unparsed("check", alert.check()),
            Placeholder.parsed("type_segment", typeSegment),
            Placeholder.unparsed("action", action(alert.action())),
            Placeholder.unparsed("vl", formatVl(alert)),
            Placeholder.unparsed("anti_cheat_version", alert.antiCheatVersion()),
            Placeholder.unparsed("ping", alert.ping() > 0 ? alert.ping() + "ms" : "未知"),
            Placeholder.unparsed("tps", String.format(Locale.ROOT, "%.1f", alert.tps())),
            Placeholder.unparsed("client", clientLabel(alert)),
            Placeholder.unparsed("type", alert.type()),
            Placeholder.unparsed("time", TIME_FORMAT.format(Instant.ofEpochMilli(alert.timestamp()))),
            Placeholder.unparsed("description", descriptions.translate(alert)),
            Placeholder.unparsed("verbose", alert.verbose())
        );
    }

    private String action(String action) {
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "PUNISH" -> settings.actions().getOrDefault("punish", "已处罚");
            case "SETBACK" -> settings.actions().getOrDefault("setback", "回退");
            default -> settings.actions().getOrDefault("flag", "触发");
        };
    }

    private String clientLabel(AlertData alert) {
        if (alert.clientBrand().isBlank() && alert.clientVersion().isBlank()) {
            return "未知";
        }
        if (alert.clientBrand().isBlank()) {
            return alert.clientVersion();
        }
        if (alert.clientVersion().isBlank()) {
            return alert.clientBrand();
        }
        return alert.clientBrand() + " / " + alert.clientVersion();
    }

    private String formatVl(AlertData alert) {
        String vl = trim(alert.violations());
        if (alert.maxViolations() > 0) {
            return "VL " + vl + "/" + trim(alert.maxViolations());
        }
        return "VL " + vl;
    }

    private String trim(double value) {
        return value == Math.rint(value)
            ? Long.toString((long) value)
            : String.format(Locale.ROOT, "%.1f", value);
    }
}

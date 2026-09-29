package studio.ellan.anticheatbridge.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class BridgeProtocol {
    public static final String CHANNEL = "ellan:ac_bridge";
    public static final int MAGIC = 0x45414342;
    public static final int VERSION = 2;

    private static final int MAX_FIELD_BYTES = 4096;

    private BridgeProtocol() {
    }

    public static byte[] encode(PacketType type, AlertData alert) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeByte(type.ordinal());
            output.writeUTF(alert.id().toString());
            output.writeLong(alert.timestamp());
            output.writeUTF(limit(alert.server()));
            output.writeUTF(limit(alert.source()));
            output.writeUTF(limit(alert.action()));
            output.writeUTF(limit(alert.player()));
            output.writeUTF(alert.playerId() == null ? "" : alert.playerId().toString());
            output.writeUTF(limit(alert.check()));
            output.writeUTF(limit(alert.type()));
            output.writeUTF(limit(alert.description()));
            output.writeUTF(limit(alert.verbose()));
            output.writeDouble(alert.violations());
            output.writeDouble(alert.maxViolations());
            output.writeDouble(alert.tps());
            output.writeInt(alert.ping());
            output.writeUTF(limit(alert.clientBrand()));
            output.writeUTF(limit(alert.clientVersion()));
            output.writeUTF(limit(alert.antiCheatVersion()));
            output.writeBoolean(alert.experimental());
        }
        return buffer.toByteArray();
    }

    public static Envelope decode(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            if (input.readInt() != MAGIC) {
                throw new IOException("Invalid bridge magic");
            }
            int version = input.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported bridge protocol version: " + version);
            }
            int typeOrdinal = input.readUnsignedByte();
            PacketType[] types = PacketType.values();
            if (typeOrdinal >= types.length) {
                throw new IOException("Invalid packet type: " + typeOrdinal);
            }
            UUID id = UUID.fromString(input.readUTF());
            long timestamp = input.readLong();
            String server = input.readUTF();
            String source = input.readUTF();
            String action = input.readUTF();
            String player = input.readUTF();
            String playerIdRaw = input.readUTF();
            UUID playerId = playerIdRaw.isEmpty() ? null : UUID.fromString(playerIdRaw);
            String check = input.readUTF();
            String type = input.readUTF();
            String description = input.readUTF();
            String verbose = input.readUTF();
            double violations = input.readDouble();
            double maxViolations = input.readDouble();
            double tps = input.readDouble();
            int ping = input.readInt();
            String clientBrand = input.readUTF();
            String clientVersion = input.readUTF();
            String antiCheatVersion = input.readUTF();
            boolean experimental = input.readBoolean();

            return new Envelope(
                types[typeOrdinal],
                new AlertData(id, timestamp, server, source, action, player, playerId,
                    check, type, description, verbose, violations, maxViolations, tps, ping,
                    clientBrand, clientVersion, antiCheatVersion, experimental)
            );
        }
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        if (value.getBytes(StandardCharsets.UTF_8).length <= MAX_FIELD_BYTES) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            String part = new String(Character.toChars(codePoint));
            int partBytes = part.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + partBytes > MAX_FIELD_BYTES - 3) {
                break;
            }
            builder.append(part);
            bytes += partBytes;
            offset += Character.charCount(codePoint);
        }
        return builder.append("...").toString();
    }

    public record Envelope(PacketType type, AlertData alert) {
    }
}

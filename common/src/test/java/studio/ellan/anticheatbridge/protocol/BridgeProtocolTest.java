package studio.ellan.anticheatbridge.protocol;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BridgeProtocolTest {
    @Test
    void roundTripsAlertData() throws Exception {
        AlertData source = new AlertData(
            UUID.randomUUID(),
            123456789L,
            "test",
            "Vulcan",
            "FLAG",
            "Player",
            UUID.randomUUID(),
            "ReachA",
            "A",
            "Attack range",
            "distance=4.1",
            4.0,
            10.0,
            false
        );

        byte[] encoded = BridgeProtocol.encode(PacketType.REPORT, source);
        BridgeProtocol.Envelope decoded = BridgeProtocol.decode(encoded);

        assertEquals(PacketType.REPORT, decoded.type());
        assertEquals(source, decoded.alert());
    }

    @Test
    void rejectsInvalidProtocol() {
        assertThrows(Exception.class, () -> BridgeProtocol.decode(new byte[]{1, 2, 3}));
    }
}

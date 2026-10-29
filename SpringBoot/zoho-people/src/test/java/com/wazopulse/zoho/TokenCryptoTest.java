package com.wazopulse.zoho;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenCryptoTest {
    private final TokenCrypto crypto = new TokenCrypto(TestProps.props());

    @Test
    void roundTrips() {
        String enc = crypto.encrypt("1000.refresh.token");
        assertNotEquals("1000.refresh.token", enc);
        assertEquals("1000.refresh.token", crypto.decrypt(enc));
    }

    @Test
    void sameInputGivesDifferentCiphertext() {
        assertNotEquals(crypto.encrypt("x"), crypto.encrypt("x"));
    }

    @Test
    void tamperedCiphertextFails() {
        byte[] raw = Base64.getDecoder().decode(crypto.encrypt("secret"));
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(tampered));
    }

    @Test
    void rejectsShortKey() {
        ZohoProperties bad = new ZohoProperties("a", "b", "c",
                Base64.getEncoder().encodeToString(new byte[16]), "s", "f", null, null, null, null);
        assertThrows(IllegalStateException.class, () -> new TokenCrypto(bad));
    }
}

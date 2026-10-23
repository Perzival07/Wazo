package com.example.slackrequestapp.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlackSignatureVerifierTest {

    private static final String SECRET = "test-secret";
    private SlackSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new SlackSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "secret", SECRET);
    }

    private static String sign(String ts, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(UTF_8), "HmacSHA256"));
        return "v0=" + HexFormat.of().formatHex(mac.doFinal(("v0:" + ts + ":" + body).getBytes(UTF_8)));
    }

    @Test
    void acceptsValidSignature() throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String body = "{\"type\":\"event_callback\"}";
        assertTrue(verifier.valid(ts, sign(ts, body), body));
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String sig = sign(ts, "original");
        assertFalse(verifier.valid(ts, sig, "tampered"));
    }

    @Test
    void rejectsWrongSecret() {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        assertFalse(verifier.valid(ts, "v0=deadbeef", "body"));
    }

    @Test
    void rejectsOldTimestamp() throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond() - 600);   // 10 min old
        String body = "body";
        assertFalse(verifier.valid(ts, sign(ts, body), body));
    }

    @Test
    void rejectsMissingHeaders() {
        assertFalse(verifier.valid(null, null, "body"));
    }
}

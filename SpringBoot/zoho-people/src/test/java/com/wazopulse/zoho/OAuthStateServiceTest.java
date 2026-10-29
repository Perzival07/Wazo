package com.wazopulse.zoho;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OAuthStateServiceTest {
    private static final Instant T0 = Instant.parse("2026-10-27T10:00:00Z");
    private final CurrentUser user = new CurrentUser("tenant-1", "user-9", "priya@acme.com");

    private OAuthStateService at(Instant now) {
        return new OAuthStateService(TestProps.props(), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void roundTrip() {
        String state = at(T0).create(user, "nonce-abc");
        assertEquals(user, at(T0.plusSeconds(30)).verify(state, "nonce-abc"));
    }

    @Test
    void wrongNonceRejected() {
        String state = at(T0).create(user, "nonce-abc");
        var ex = assertThrows(ZohoExceptions.OAuthCallback.class, () -> at(T0).verify(state, "other"));
        assertEquals("invalid_state", ex.reason());
    }

    @Test
    void expiredRejected() {
        String state = at(T0).create(user, "n");
        var ex = assertThrows(ZohoExceptions.OAuthCallback.class,
                () -> at(T0.plus(Duration.ofMinutes(11))).verify(state, "n"));
        assertEquals("state_expired", ex.reason());
    }

    @Test
    void tamperedRejected() {
        String state = at(T0).create(user, "n");
        String tampered = "x" + state;
        assertThrows(ZohoExceptions.OAuthCallback.class, () -> at(T0).verify(tampered, "n"));
    }

    @Test
    void missingValuesRejected() {
        assertThrows(ZohoExceptions.OAuthCallback.class, () -> at(T0).verify(null, "n"));
        assertThrows(ZohoExceptions.OAuthCallback.class, () -> at(T0).verify("abc.def", null));
    }
}

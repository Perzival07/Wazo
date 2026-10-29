package com.wazopulse.zoho;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * state = base64url(payload) "." base64url(HMAC(payload)).
 * payload = tenant:user:email:nonce:expiry. The nonce must also match the HttpOnly cookie
 * set at connect time, so a state link alone cannot be replayed from another browser.
 */
@Component
public class OAuthStateService {
    private static final long TTL_SECONDS = 600;

    private final byte[] key;
    private final Clock clock;

    public OAuthStateService(ZohoProperties props, Clock clock) {
        this.key = props.stateSigningKey().getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String create(CurrentUser u, String nonce) {
        long exp = clock.instant().getEpochSecond() + TTL_SECONDS;
        String payload = String.join(":",
                enc(u.tenantId()), enc(u.userId()), enc(u.email()), enc(nonce), Long.toString(exp));
        return enc(payload) + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
    }

    public CurrentUser verify(String state, String nonceFromCookie) {
        try {
            if (state == null || nonceFromCookie == null) throw bad();
            String[] parts = state.split("\\.");
            if (parts.length != 2) throw bad();
            String payload = dec(parts[0]);
            byte[] sig = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), sig)) throw bad();

            String[] f = payload.split(":");
            if (f.length != 5) throw bad();
            if (Long.parseLong(f[4]) < clock.instant().getEpochSecond()) {
                throw new ZohoExceptions.OAuthCallback("state_expired");
            }
            String nonce = dec(f[3]);
            if (!MessageDigest.isEqual(
                    nonce.getBytes(StandardCharsets.UTF_8),
                    nonceFromCookie.getBytes(StandardCharsets.UTF_8))) {
                throw bad();
            }
            return new CurrentUser(dec(f[0]), dec(f[1]), dec(f[2]));
        } catch (IllegalArgumentException e) { // malformed base64 or number
            throw bad();
        }
    }

    private static ZohoExceptions.OAuthCallback bad() {
        return new ZohoExceptions.OAuthCallback("invalid_state");
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }
}

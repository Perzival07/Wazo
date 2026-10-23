package com.example.slackrequestapp.slack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class SlackSignatureVerifier {

    @Value("${slack.signing-secret}")
    private String secret;

    public boolean valid(String timestamp, String signature, String body) {
        try {
            if (timestamp == null || signature == null) return false;

            // reject requests older than 5 minutes (replay protection)
            long ts = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - ts) > 300) return false;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
            String expected = "v0=" + HexFormat.of().formatHex(
                    mac.doFinal(("v0:" + timestamp + ":" + body).getBytes(UTF_8)));

            return MessageDigest.isEqual(expected.getBytes(UTF_8), signature.getBytes(UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}

package com.example.slackrequestapp.controller;

import com.example.slackrequestapp.service.SlackEventHandler;
import com.example.slackrequestapp.slack.SlackSignatureVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class SlackWebhookController {

    private final SlackSignatureVerifier verifier;
    private final SlackEventHandler handler;
    private final ObjectMapper om;

    public SlackWebhookController(SlackSignatureVerifier verifier, SlackEventHandler handler, ObjectMapper om) {
        this.verifier = verifier;
        this.handler = handler;
        this.om = om;
    }

    /** Events API: message events and the URL verification handshake. */
    @PostMapping("/slack/events")
    public ResponseEntity<?> events(
            @RequestBody String body,                       // raw body, needed for the signature check
            @RequestHeader("X-Slack-Request-Timestamp") String ts,
            @RequestHeader("X-Slack-Signature") String sig) throws Exception {

        if (!verifier.valid(ts, sig, body)) return ResponseEntity.status(401).build();

        JsonNode json = om.readTree(body);

        if ("url_verification".equals(json.path("type").asString())) {
            return ResponseEntity.ok(Map.of("challenge", json.path("challenge").asString()));
        }

        handler.handleEvent(json);          // @Async: returns immediately (Slack expects a reply within 3s)
        return ResponseEntity.ok().build();
    }

    /** Interactivity: Approve / Reject button clicks (form-encoded body: payload=<json>). */
    @PostMapping(value = "/slack/interactions", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> interactions(
            HttpServletRequest request,                     // raw stream: @RequestBody would re-encode form bodies and break the signature
            @RequestHeader("X-Slack-Request-Timestamp") String ts,
            @RequestHeader("X-Slack-Signature") String sig) throws Exception {

        String body = new String(request.getInputStream().readAllBytes(), UTF_8);
        if (!verifier.valid(ts, sig, body)) return ResponseEntity.status(401).build();

        String payload = Arrays.stream(body.split("&"))
                .filter(p -> p.startsWith("payload="))
                .map(p -> URLDecoder.decode(p.substring("payload=".length()), UTF_8))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing payload"));

        handler.handleInteraction(om.readTree(payload));
        return ResponseEntity.ok().build();
    }
}
package com.example.calendartest;

import com.example.calendartest.entity.CalendarEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

@Component
public class WebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessor.class);

    // Outlook ids are URL-safe base64 text. Anything else is rejected.
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9_=\\-]{10,512}$");

    private final ObjectMapper mapper;
    private final EventSyncService sync;
    private final SubscriptionService subscriptions;
    private final AppState state;

    @Value("${app.webhook.client-state}")
    private String clientState;

    public WebhookProcessor(ObjectMapper mapper, EventSyncService sync,
                            SubscriptionService subscriptions, AppState state) {
        this.mapper = mapper;
        this.sync = sync;
        this.subscriptions = subscriptions;
        this.state = state;
    }

    @PostConstruct
    void checkSecret() {
        if (clientState == null || clientState.length() < 32 || clientState.length() > 128) {
            throw new IllegalStateException(
                "WEBHOOK_CLIENT_STATE must be 32 to 128 characters. Generate one with: openssl rand -hex 32");
        }
    }

    private boolean validClientState(String received) {
        if (received == null) return false;
        return MessageDigest.isEqual(
                clientState.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));   // constant-time comparison
    }

    @Async
    public void handle(String json) {
        if (!state.isSignedIn()) {
            log.warn("Notification received before sign-in finished; the startup sync will catch up");
            return;
        }
        try {
            JsonNode root = mapper.readTree(json);
            for (JsonNode n : root.path("value")) {
                if (!validClientState(n.path("clientState").asText(null))) {
                    log.warn("Rejected notification with wrong clientState");
                    continue;
                }
                String eventId = extractEventId(n);
                if (eventId == null || !SAFE_ID.matcher(eventId).matches()) {
                    log.warn("Ignored notification with an unsafe or missing event id");
                    continue;
                }
                log.info("Webhook hint: {} {}", n.path("changeType").asText(), eventId);

                // Never trust changeType. Ask Graph what is true now:
                // exists -> save it, 404 -> mark deleted.
                sync.fetchAndUpsert(eventId, CalendarEvent.Source.OUTLOOK);
            }
        } catch (Exception e) {
            log.error("Failed to process notification", e);
        }
    }

    @Async
    public void handleLifecycle(String json) {
        if (!state.isSignedIn()) return;
        try {
            JsonNode root = mapper.readTree(json);
            for (JsonNode n : root.path("value")) {
                if (!validClientState(n.path("clientState").asText(null))) {
                    log.warn("Rejected lifecycle notification with wrong clientState");
                    continue;
                }
                String event = n.path("lifecycleEvent").asText();
                log.info("Lifecycle event: {}", event);
                switch (event) {
                    case "reauthorizationRequired" -> subscriptions.forceRenew();
                    case "subscriptionRemoved" -> subscriptions.recreate();
                    case "missed" -> sync.fullSync();
                    default -> { }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process lifecycle notification", e);
        }
    }

    private String extractEventId(JsonNode n) {
        String id = n.path("resourceData").path("id").asText(null);
        if (id != null && !id.isBlank()) return id;

        String resource = n.path("resource").asText("");
        int slash = resource.lastIndexOf('/');
        String last = slash >= 0 ? resource.substring(slash + 1) : resource;
        last = last.replaceAll("^Events\\('?|'?\\)$", "");
        return last.isBlank() ? null : last;
    }
}
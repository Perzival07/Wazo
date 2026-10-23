package com.example.calendartest;

import com.example.calendartest.entity.GraphSubscription;
import com.example.calendartest.repository.GraphSubscriptionRepository;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final long LIFETIME_MINUTES = 4200;   // Outlook event subscriptions must be under about 3 days

    private final GraphServiceClient graph;
    private final GraphSubscriptionRepository repo;

    @Value("${app.webhook.public-url:}")
    private String publicUrl;

    @Value("${app.webhook.client-state}")
    private String clientState;

    public SubscriptionService(GraphServiceClient graph, GraphSubscriptionRepository repo) {
        this.graph = graph;
        this.repo = repo;
    }

    /** Makes sure exactly one healthy subscription exists. Safe to call repeatedly. */
    public synchronized GraphSubscription ensure() {
        if (publicUrl == null || publicUrl.isBlank()) {
            log.warn("app.webhook.public-url is empty (WEBHOOK_PUBLIC_URL not set). Skipping webhook subscription.");
            return null;
        }
        GraphSubscription current = repo.findAll().stream().findFirst().orElse(null);
        if (current != null) {
            if (!current.getNotificationUrl().equals(notificationUrl())) {
                log.info("Tunnel URL changed, recreating subscription");
                deleteQuietly(current);
            } else if (current.getExpiresAt().isAfter(Instant.now().plus(Duration.ofHours(12)))) {
                return current;
            } else {
                try {
                    return renew(current);
                } catch (Exception ex) {
                    log.warn("Renewal failed ({}), recreating subscription", ex.getMessage());
                    deleteQuietly(current);
                }
            }
        }
        return create();
    }

    public synchronized GraphSubscription forceRenew() {
        GraphSubscription current = repo.findAll().stream().findFirst().orElse(null);
        return current == null ? ensure() : renew(current);
    }

    public synchronized GraphSubscription recreate() {
        repo.findAll().forEach(this::deleteQuietly);
        return create();
    }

    public GraphSubscription current() {
        return repo.findAll().stream().findFirst().orElse(null);
    }

    private GraphSubscription create() {
        Subscription s = new Subscription();
        s.setChangeType("created,updated,deleted");
        s.setNotificationUrl(notificationUrl());
        s.setLifecycleNotificationUrl(lifecycleUrl());
        s.setResource("me/events");
        s.setExpirationDateTime(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(LIFETIME_MINUTES));
        s.setClientState(clientState);

        Subscription created = graph.subscriptions().post(s);

        GraphSubscription row = new GraphSubscription();
        row.setGraphId(created.getId());
        row.setResource("me/events");
        row.setNotificationUrl(notificationUrl());
        row.setExpiresAt(created.getExpirationDateTime().toInstant());
        log.info("Subscription created: {}", created.getId());
        return repo.save(row);
    }

    private GraphSubscription renew(GraphSubscription row) {
        Subscription patch = new Subscription();
        patch.setExpirationDateTime(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(LIFETIME_MINUTES));
        Subscription updated = graph.subscriptions().bySubscriptionId(row.getGraphId()).patch(patch);
        row.setExpiresAt(updated.getExpirationDateTime().toInstant());
        log.info("Subscription renewed: {}", row.getGraphId());
        return repo.save(row);
    }

    private void deleteQuietly(GraphSubscription row) {
        try {
            graph.subscriptions().bySubscriptionId(row.getGraphId()).delete();
        } catch (Exception ex) {
            log.debug("Could not delete subscription {} in Graph: {}", row.getGraphId(), ex.getMessage());
        }
        repo.delete(row);
    }

    private String base() {
        return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    private String notificationUrl() { return base() + "/api/webhooks/graph"; }

    private String lifecycleUrl() { return base() + "/api/webhooks/lifecycle"; }
}
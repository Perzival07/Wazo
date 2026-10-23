package com.example.calendartest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final AppState state;
    private final EventSyncService sync;
    private final SubscriptionService subscriptions;

    public ScheduledJobs(AppState state, EventSyncService sync, SubscriptionService subscriptions) {
        this.state = state;
        this.sync = sync;
        this.subscriptions = subscriptions;
    }

    // every 6 hours: renew the webhook subscription if it is close to expiring
    @Scheduled(fixedDelay = 21_600_000, initialDelay = 21_600_000)
    public void renewSubscription() {
        if (!state.isSignedIn()) return;
        try {
            subscriptions.ensure();
        } catch (Exception e) {
            log.error("Subscription renewal failed: {}", e.getMessage());
        }
    }

    // every 15 minutes: catch anything a webhook missed
    @Scheduled(fixedDelay = 900_000, initialDelay = 900_000)
    public void periodicSync() {
        if (!state.isSignedIn()) return;
        try {
            sync.fullSync();
        } catch (Exception e) {
            log.error("Periodic sync failed: {}", e.getMessage());
        }
    }
}
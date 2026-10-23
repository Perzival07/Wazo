package com.example.calendartest;

import com.example.calendartest.entity.GraphSubscription;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EventSyncService sync;
    private final SubscriptionService subscriptions;

    public AdminController(EventSyncService sync, SubscriptionService subscriptions) {
        this.sync = sync;
        this.subscriptions = subscriptions;
    }

    @PostMapping("/sync")
    public Map<String, Object> sync() {
        return Map.of("eventsSeenInOutlook", sync.fullSync());
    }

    @PostMapping("/subscription/ensure")
    public Object ensure() {
        GraphSubscription s = subscriptions.ensure();
        return s != null ? s : Map.of("message", "No subscription created. Is WEBHOOK_PUBLIC_URL set?");
    }

    @GetMapping("/subscription")
    public Object current() {
        GraphSubscription s = subscriptions.current();
        return s != null ? s : Map.of("message", "No subscription stored");
    }
}
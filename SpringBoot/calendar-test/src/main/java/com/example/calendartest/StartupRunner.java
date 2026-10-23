package com.example.calendartest;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final GraphServiceClient graph;
    private final AppState state;
    private final EventSyncService sync;
    private final SubscriptionService subscriptions;

    public StartupRunner(GraphServiceClient graph, AppState state,
                         EventSyncService sync, SubscriptionService subscriptions) {
        this.graph = graph;
        this.state = state;
        this.sync = sync;
        this.subscriptions = subscriptions;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Own thread, so the web server stays responsive while you complete the device login
        new Thread(this::bootstrap, "startup-bootstrap").start();
    }

    private void bootstrap() {
        try {
            log.info("Signing in to Microsoft Graph. Watch for the device login message below.");
            graph.me().calendar().get();   // first call, triggers the device code prompt
            state.setSignedIn(true);
            log.info("Signed in.");

            sync.fullSync();
            subscriptions.ensure();
        } catch (Exception e) {
            log.error("Startup bootstrap failed: {}", e.getMessage(), e);
        }
    }
}



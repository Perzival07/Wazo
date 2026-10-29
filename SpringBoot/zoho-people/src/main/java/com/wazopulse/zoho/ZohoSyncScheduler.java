package com.wazopulse.zoho;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically refreshes the local employee copy for every connected Zoho user. */
@Component
public class ZohoSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(ZohoSyncScheduler.class);

    private final ZohoConnectionRepository connections;
    private final ZohoPeopleService service;

    public ZohoSyncScheduler(ZohoConnectionRepository connections, ZohoPeopleService service) {
        this.connections = connections;
        this.service = service;
    }

    @Scheduled(initialDelayString = "${zoho.sync-interval-ms:900000}", fixedDelayString = "${zoho.sync-interval-ms:900000}")
    public void syncAll() {
        // One connection per tenant is enough: the copy is keyed by tenant, not by user.
        java.util.Set<String> done = new java.util.HashSet<>();
        for (ZohoConnection c : connections.findAll()) {
            if (c.getStatus() != ConnectionStatus.CONNECTED || !done.add(c.getTenantId())) continue;
            try {
                service.syncForm(new CurrentUser(c.getTenantId(), c.getUserId(), c.getZohoEmail()), "employee");
            } catch (RuntimeException e) {
                done.remove(c.getTenantId());   // let another connection of this tenant try
                log.warn("Zoho sync failed for tenant {}: {}", c.getTenantId(), e.getMessage());
            }
        }
    }
}

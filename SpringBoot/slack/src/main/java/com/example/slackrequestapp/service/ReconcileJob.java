package com.example.slackrequestapp.service;

import com.example.slackrequestapp.entity.RequestEntity;
import com.example.slackrequestapp.repository.RequestRepository;
import com.slack.api.methods.MethodsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Safety net: pulls from Slack and fixes anything the webhook missed
 * (for example, a message deleted while the app was down).
 */
@Component
@Slf4j
public class ReconcileJob {

    private final RequestRepository repo;
    private final MethodsClient slack;

    public ReconcileJob(RequestRepository repo, MethodsClient slack) {
        this.repo = repo;
        this.slack = slack;
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    @Transactional
    public void reconcile() throws Exception {
        for (RequestEntity e : repo.findByStatus(RequestEntity.Status.PENDING)) {
            if (e.getSlackTs() == null) continue;

            var res = slack.conversationsHistory(r -> r
                    .channel(e.getSlackChannel())
                    .latest(e.getSlackTs())
                    .inclusive(true)
                    .limit(1));

            if (!res.isOk()) {
                log.warn("conversations.history failed for request {}: {}", e.getId(), res.getError());
                continue;
            }

            boolean exists = res.getMessages() != null
                    && !res.getMessages().isEmpty()
                    && e.getSlackTs().equals(res.getMessages().get(0).getTs());

            if (!exists) {
                log.info("Slack message for request {} is gone, marking CANCELLED", e.getId());
                e.setStatus(RequestEntity.Status.CANCELLED);
            }
        }
    }
}
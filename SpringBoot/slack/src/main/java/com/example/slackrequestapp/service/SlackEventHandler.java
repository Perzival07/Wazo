package com.example.slackrequestapp.service;

import com.example.slackrequestapp.entity.RequestEntity;
import com.example.slackrequestapp.repository.RequestRepository;
import tools.jackson.databind.JsonNode;
import com.slack.api.methods.MethodsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SlackEventHandler {

    private final RequestRepository repo;
    private final JdbcTemplate jdbc;
    private final MethodsClient slack;

    public SlackEventHandler(RequestRepository repo, JdbcTemplate jdbc, MethodsClient slack) {
        this.repo = repo;
        this.jdbc = jdbc;
        this.slack = slack;
    }

    @Async
    @Transactional
    public void handleEvent(JsonNode root) {
        String eventId = root.path("event_id").asString();

        // Slack retries failed deliveries; the primary key makes a duplicate a no-op.
        int inserted = jdbc.update(
                "insert into slack_event_log(event_id) values (?) on conflict do nothing", eventId);
        if (inserted == 0) {
            log.debug("Duplicate Slack event {}, skipping", eventId);
            return;
        }

        JsonNode ev = root.path("event");
        if (ev.has("bot_id")) return;                       // ignore our own bot's messages

        String subtype = ev.path("subtype").asString("");

        if ("message_deleted".equals(subtype)) {
            repo.findBySlackChannelAndSlackTs(
                            ev.path("channel").asString(),
                            ev.path("deleted_ts").asString())
                    .ifPresent(r -> r.setStatus(RequestEntity.Status.CANCELLED));
        }

        if ("message_changed".equals(subtype)) {
            JsonNode msg = ev.path("message");
            if (msg.has("bot_id")) return;                  // echo of our own chat.update, ignore

            // A human edited a message: keep the stored copy in sync.
            jdbc.update("update slack_message set text = ? where channel = ? and ts = ?",
                    msg.path("text").asString(), ev.path("channel").asString(), msg.path("ts").asString());
        }

        if ("message".equals(ev.path("type").asString()) && subtype.isEmpty()) {
            jdbc.update("insert into slack_message(channel, ts, user_id, text) values (?, ?, ?, ?) "
                            + "on conflict do nothing",
                    ev.path("channel").asString(), ev.path("ts").asString(),
                    ev.path("user").asString(), ev.path("text").asString());
        }

        log.info("Slack event {} type={} subtype={}", eventId, ev.path("type").asString(), subtype);
    }

    @Async
    @Transactional
    public void handleInteraction(JsonNode payload) throws Exception {
        JsonNode action = payload.path("actions").path(0);
        Long id = Long.valueOf(action.path("value").asString());

        RequestEntity.Status newStatus = "approve".equals(action.path("action_id").asString())
                ? RequestEntity.Status.APPROVED
                : RequestEntity.Status.REJECTED;

        RequestEntity e = repo.findById(id).orElse(null);
        if (e == null || e.getStatus() != RequestEntity.Status.PENDING) return;   // unknown or already decided

        e.setStatus(newStatus);
        e.setDecidedBy(payload.path("user").path("id").asString());

        // Re-render the Slack message without the buttons
        var res = slack.chatUpdate(r -> r
                .channel(e.getSlackChannel())
                .ts(e.getSlackTs())
                .text("Request " + newStatus)
                .blocks(RequestService.blocks(e, false)));

        if (!res.isOk()) log.warn("chat.update failed: {}", res.getError());
    }
}
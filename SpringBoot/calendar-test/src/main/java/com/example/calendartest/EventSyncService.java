package com.example.calendartest;

import com.example.calendartest.entity.CalendarEvent;
import com.example.calendartest.repository.CalendarEventRepository;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.EventCollectionResponse;
import com.microsoft.graph.models.odataerrors.ODataError;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class EventSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventSyncService.class);
    private static final String UTC_HEADER = "outlook.timezone=\"UTC\"";

    private final GraphServiceClient graph;
    private final CalendarEventRepository repo;

    public EventSyncService(GraphServiceClient graph, CalendarEventRepository repo) {
        this.graph = graph;
        this.repo = repo;
    }

    /** Reads one event from Outlook and saves it. Used by local create and by webhooks. */
    public CalendarEvent fetchAndUpsert(String outlookId, CalendarEvent.Source sourceIfNew) {
        try {
            Event e = graph.me().events().byEventId(outlookId)
                    .get(cfg -> cfg.headers.add("Prefer", UTC_HEADER));
            return upsert(e, sourceIfNew);
        } catch (ODataError err) {
            if (err.getResponseStatusCode() == 404) {
                markDeleted(outlookId);
                return null;
            }
            throw err;
        }
    }

    private synchronized CalendarEvent upsert(Event e, CalendarEvent.Source sourceIfNew) {
        if (Boolean.TRUE.equals(e.getIsCancelled())) {
            markDeleted(e.getId());
            return null;
        }
        CalendarEvent row = repo.findByOutlookEventId(e.getId()).orElse(null);

        // Nothing changed since last time (for example, the echo of our own create)
        if (row != null && row.getStatus() == CalendarEvent.Status.ACTIVE
                && Objects.equals(row.getChangeKey(), e.getChangeKey())) {
            return row;
        }
        if (row == null) {
            row = new CalendarEvent();
            row.setOutlookEventId(e.getId());
            row.setSource(sourceIfNew);
        }
        row.setStatus(CalendarEvent.Status.ACTIVE);
        row.setIcalUid(e.getICalUId());
        row.setSubject(e.getSubject());
        row.setStartTime(toInstant(e.getStart()));
        row.setEndTime(toInstant(e.getEnd()));
        row.setWebLink(e.getWebLink());
        row.setChangeKey(e.getChangeKey());
        row.setLastSyncedAt(Instant.now());
        return repo.save(row);
    }

    public synchronized void markDeleted(String outlookId) {
        if (outlookId == null) return;
        repo.findByOutlookEventId(outlookId).ifPresent(row -> {
            if (row.getStatus() != CalendarEvent.Status.DELETED) {
                row.setStatus(CalendarEvent.Status.DELETED);
                row.setLastSyncedAt(Instant.now());
                repo.save(row);
            }
        });
    }

    /** Pulls every event in a window around today and saves it. Also marks vanished events as deleted. */
    public int fullSync() {
        Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(180, ChronoUnit.DAYS);
        Set<String> seen = new HashSet<>();

        EventCollectionResponse page = graph.me().calendarView().get(cfg -> {
            cfg.queryParameters.startDateTime = from.toString();
            cfg.queryParameters.endDateTime = to.toString();
            cfg.queryParameters.top = 50;
            cfg.headers.add("Prefer", UTC_HEADER);
        });

        while (page != null) {
            if (page.getValue() != null) {
                for (Event e : page.getValue()) {
                    seen.add(e.getId());
                    upsert(e, CalendarEvent.Source.OUTLOOK);
                }
            }
            String next = page.getOdataNextLink();
            page = (next == null) ? null
                    : graph.me().calendarView().withUrl(next)
                            .get(cfg -> cfg.headers.add("Prefer", UTC_HEADER));
        }

        repo.findByStatusAndStartTimeBetween(CalendarEvent.Status.ACTIVE, from, to).stream()
                .filter(r -> r.getOutlookEventId() != null && !seen.contains(r.getOutlookEventId()))
                .forEach(r -> markDeleted(r.getOutlookEventId()));

        log.info("Full sync finished: {} events seen in Outlook", seen.size());
        return seen.size();
    }

    private Instant toInstant(DateTimeTimeZone d) {
        if (d == null || d.getDateTime() == null) return null;
        return LocalDateTime.parse(d.getDateTime()).toInstant(ZoneOffset.UTC);
    }
}
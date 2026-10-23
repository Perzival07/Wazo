package com.example.calendartest;

import com.example.calendartest.entity.CalendarEvent;
import com.example.calendartest.repository.CalendarEventRepository;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.odataerrors.ODataError;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CalendarService {

    private final GraphServiceClient graph;
    private final EventSyncService sync;
    private final CalendarEventRepository repo;

    public CalendarService(GraphServiceClient graph, EventSyncService sync, CalendarEventRepository repo) {
        this.graph = graph;
        this.sync = sync;
        this.repo = repo;
    }

    public CalendarEvent create(EventRequest r) {
        String tz = r.timeZone() != null ? r.timeZone() : "India Standard Time";
        Event event = new Event();
        event.setSubject(r.subject());
        event.setStart(dt(r.start(), tz));
        event.setEnd(dt(r.end(), tz));

        Event created = graph.me().events().post(event);
        return sync.fetchAndUpsert(created.getId(), CalendarEvent.Source.LOCAL);
    }

    public List<CalendarEvent> list() {
        return repo.findByStatusOrderByStartTimeAsc(CalendarEvent.Status.ACTIVE);
    }

    public CalendarEvent get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    public void delete(Long id) {
        CalendarEvent row = get(id);
        if (row.getOutlookEventId() != null && row.getStatus() == CalendarEvent.Status.ACTIVE) {
            try {
                graph.me().events().byEventId(row.getOutlookEventId()).delete();
            } catch (ODataError e) {
                if (e.getResponseStatusCode() != 404) throw e;
            }
        }
        sync.markDeleted(row.getOutlookEventId());
    }

    private DateTimeTimeZone dt(LocalDateTime t, String tz) {
        DateTimeTimeZone d = new DateTimeTimeZone();
        d.setDateTime(t.toString());
        d.setTimeZone(tz);
        return d;
    }
}
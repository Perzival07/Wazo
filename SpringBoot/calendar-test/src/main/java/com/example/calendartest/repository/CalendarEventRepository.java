package com.example.calendartest.repository;

import com.example.calendartest.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    Optional<CalendarEvent> findByOutlookEventId(String outlookEventId);

    List<CalendarEvent> findByStatusOrderByStartTimeAsc(CalendarEvent.Status status);

    List<CalendarEvent> findByStatusAndStartTimeBetween(CalendarEvent.Status status, Instant from, Instant to);
}


package com.example.calendartest;

import com.example.calendartest.entity.CalendarEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final CalendarService service;

    public EventController(CalendarService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEvent create(@RequestBody EventRequest r) {
        return service.create(r);
    }

    @GetMapping
    public List<CalendarEvent> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CalendarEvent get(@PathVariable Long id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
package com.example.calendartest;

import java.time.LocalDateTime;

public record EventRequest(String subject, LocalDateTime start, LocalDateTime end, String timeZone) {}

package com.example.calendartest.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    public enum Source {LOCAL, OUTLOOK}
    public enum Status {ACTIVE, DELETED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outlook_event_id", unique = true, length = 512)
    private String outlookEventId;

    @Column(name = "ical_uid", length = 512)
    private String icalUid;

    @Column(length = 1024)
    private String subject;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "web_link", length = 2048)
    private  String webLink;

    @Column(name = "cnage_key", length = 2048)
    private String changeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now;}

    @PreUpdate 
    void onUpdate() { updatedAt = Instant.now(); }


    public Long getId() { return id; }
    public String getOutlookEventId() { return outlookEventId; }
    public void setOutlookEventId(String v) { this.outlookEventId = v; }
    public String getIcalUid() { return icalUid; }
    public void setIcalUid(String v) { this.icalUid = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant v) { this.startTime = v; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant v) { this.endTime = v; }
    public String getWebLink() { return webLink; }
    public void setWebLink(String v) { this.webLink = v; }
    public String getChangeKey() { return changeKey; }
    public void setChangeKey(String v) { this.changeKey = v; }
    public Source getSource() { return source; }
    public void setSource(Source v) { this.source = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant v) { this.lastSyncedAt = v; }

}

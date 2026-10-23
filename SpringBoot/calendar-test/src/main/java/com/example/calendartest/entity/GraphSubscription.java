package com.example.calendartest.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "graph_subscription")
public class GraphSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "graph_id", unique = true, nullable = false)
    private String graphId;

    @Column(nullable = false)
    private String resource;

    @Column(name = "notification_url", nullable = false, length = 1024)
    private String notificationUrl;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getGraphId() { return graphId; }
    public void setGraphId(String v) { this.graphId = v; }
    public String getResource() { return resource; }
    public void setResource(String v) { this.resource = v; }
    public String getNotificationUrl() { return notificationUrl; }
    public void setNotificationUrl(String v) { this.notificationUrl = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}



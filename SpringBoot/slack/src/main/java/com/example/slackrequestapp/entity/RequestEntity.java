package com.example.slackrequestapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "request")
@Getter
@Setter
@NoArgsConstructor
public class RequestEntity {

    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String slackChannel;
    private String slackTs;
    private String decidedBy;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public RequestEntity(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
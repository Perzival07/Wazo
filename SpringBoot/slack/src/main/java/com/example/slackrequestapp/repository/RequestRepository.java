package com.example.slackrequestapp.repository;

import com.example.slackrequestapp.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<RequestEntity, Long> {
    Optional<RequestEntity> findBySlackChannelAndSlackTs(String channel, String ts);
    List<RequestEntity> findByStatus(RequestEntity.Status status);
}
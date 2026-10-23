package com.example.slackrequestapp.repository;

import com.example.slackrequestapp.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, Long> {
    List<SlackMessage> findByChannelOrderByTsDesc(String channel);
}

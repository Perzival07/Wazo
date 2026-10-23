package com.example.slackrequestapp.controller;

import com.example.slackrequestapp.entity.SlackMessage;
import com.example.slackrequestapp.repository.SlackMessageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class SlackMessageController {

    private final SlackMessageRepository repo;

    public SlackMessageController(SlackMessageRepository repo) {
        this.repo = repo;
    }

    /** All stored channel messages, newest first; optionally filtered by channel id. */
    @GetMapping
    public List<SlackMessage> all(@RequestParam(required = false) String channel) {
        return channel == null
                ? repo.findAll(Sort.by(Sort.Direction.DESC, "ts"))
                : repo.findByChannelOrderByTsDesc(channel);
    }
}

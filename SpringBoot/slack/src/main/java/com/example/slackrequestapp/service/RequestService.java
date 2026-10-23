package com.example.slackrequestapp.service;

import com.example.slackrequestapp.dto.CreateRequestDto;
import com.example.slackrequestapp.dto.UpdateRequestDto;
import com.example.slackrequestapp.entity.RequestEntity;
import com.example.slackrequestapp.repository.RequestRepository;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.block.LayoutBlock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static com.slack.api.model.block.Blocks.actions;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

@Service
public class RequestService {

    private final RequestRepository repo;
    private final MethodsClient slack;

    public RequestService(RequestRepository repo, MethodsClient slack) {
        this.repo = repo;
        this.slack = slack;
    }

    @Value("${slack.channel-id}")
    private String channel;

    @Transactional
    public RequestEntity create(CreateRequestDto dto) throws Exception {
        RequestEntity e = repo.save(new RequestEntity(dto.title(), dto.description()));

        var res = slack.chatPostMessage(r -> r
                .channel(channel)
                .text("New request: " + e.getTitle())
                .blocks(blocks(e, true)));

        if (!res.isOk()) {
            // throwing rolls back the DB row so DB and Slack never disagree
            throw new IllegalStateException("Slack error: " + res.getError());
        }
        e.setSlackChannel(res.getChannel());
        e.setSlackTs(res.getTs());
        return e;
    }

    @Transactional
    public RequestEntity update(Long id, UpdateRequestDto dto) throws Exception {
        RequestEntity e = get(id);
        if (dto.title() != null) e.setTitle(dto.title());
        if (dto.description() != null) e.setDescription(dto.description());

        var res = slack.chatUpdate(r -> r
                .channel(e.getSlackChannel())
                .ts(e.getSlackTs())
                .text("Request updated: " + e.getTitle())
                .blocks(blocks(e, e.getStatus() == RequestEntity.Status.PENDING)));

        if (!res.isOk()) {
            // throwing rolls back the DB change so DB and Slack never disagree
            throw new IllegalStateException("Slack error: " + res.getError());
        }
        return e;
    }

    @Transactional(readOnly = true)
    public List<RequestEntity> all() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public RequestEntity get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    public static List<LayoutBlock> blocks(RequestEntity e, boolean withButtons) {
        List<LayoutBlock> list = new ArrayList<>();
        String desc = e.getDescription() == null ? "" : e.getDescription();

        list.add(section(s -> s.text(markdownText(
                "*" + e.getTitle() + "*\n" + desc + "\nStatus: `" + e.getStatus() + "`"))));

        if (withButtons) {
            String id = String.valueOf(e.getId());
            list.add(actions(a -> a.elements(asElements(
                    button(b -> b.text(plainText("Approve")).actionId("approve").value(id).style("primary")),
                    button(b -> b.text(plainText("Reject")).actionId("reject").value(id).style("danger"))
            ))));
        }
        return list;
    }
}
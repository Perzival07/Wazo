package com.example.slackrequestapp.controller;

import com.example.slackrequestapp.dto.CreateRequestDto;
import com.example.slackrequestapp.dto.UpdateRequestDto;
import com.example.slackrequestapp.entity.RequestEntity;
import com.example.slackrequestapp.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }

    @PostMapping
    public RequestEntity create(@Valid @RequestBody CreateRequestDto dto) throws Exception {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public RequestEntity update(@PathVariable Long id, @RequestBody UpdateRequestDto dto) throws Exception {
        return service.update(id, dto);
    }

    @GetMapping
    public List<RequestEntity> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    public RequestEntity get(@PathVariable Long id) {
        return service.get(id);
    }
}
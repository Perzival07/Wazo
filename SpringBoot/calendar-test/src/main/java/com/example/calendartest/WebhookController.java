package com.example.calendartest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookProcessor processor;

    public WebhookController(WebhookProcessor processor) {
        this.processor = processor;
    }

    @PostMapping("/graph")
    public ResponseEntity<String> graph(
            @RequestParam(value = "validationToken", required = false) String token,
            @RequestBody(required = false) String body) {


        if (body != null && body.length() > 200_000) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        if (token != null) {   // Graph's handshake: echo the token as plain text
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(token);
        }
        if (body != null && !body.isBlank()) {
            processor.handle(body);   // runs in the background
        }
        return ResponseEntity.accepted().build();   // answer fast with 202
    }

    @PostMapping("/lifecycle")
    public ResponseEntity<String> lifecycle(
            @RequestParam(value = "validationToken", required = false) String token,
            @RequestBody(required = false) String body) {

        if (body != null && body.length() > 200_000) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        if (token != null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(token);
        }
        if (body != null && !body.isBlank()) {
            processor.handleLifecycle(body);
        }
        return ResponseEntity.accepted().build();
    }
}
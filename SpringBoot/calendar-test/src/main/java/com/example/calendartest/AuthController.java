package com.example.calendartest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(String username, String password) {}

    private final UserDetailsService users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthController(UserDetailsService users, PasswordEncoder encoder, TokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest r) {
        if (r == null || r.username() == null || r.password() == null) {
            return unauthorized();
        }
        try {
            UserDetails user = users.loadUserByUsername(r.username());
            if (!encoder.matches(r.password(), user.getPassword())) {
                return unauthorized();
            }
            return ResponseEntity.ok(Map.of(
                    "accessToken", tokens.issue(user.getUsername()),
                    "tokenType", "Bearer",
                    "expiresInSeconds", tokens.expirySeconds()));
        } catch (UsernameNotFoundException e) {
            return unauthorized();   // same answer as a wrong password, so attackers learn nothing
        }
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }
}

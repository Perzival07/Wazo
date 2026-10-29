package com.wazopulse.zoho;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/integrations/zoho-people")
public class ZohoIntegrationController {
    static final String NONCE_COOKIE = "zoho_oauth_nonce";
    private static final String COOKIE_PATH = "/api/integrations/zoho-people";

    private final CurrentUserProvider users;
    private final ZohoOAuthService oauth;
    private final ZohoPeopleService service;
    private final ZohoProperties props;

    public ZohoIntegrationController(CurrentUserProvider users, ZohoOAuthService oauth,
                                     ZohoPeopleService service, ZohoProperties props) {
        this.users = users;
        this.oauth = oauth;
        this.service = service;
        this.props = props;
    }

    @GetMapping("/status")
    public Dtos.StatusResponse status(HttpServletRequest req) {
        CurrentUser u = users.require(req);
        return service.find(u).map(ZohoIntegrationController::toStatus)
                .orElse(new Dtos.StatusResponse(false, "NOT_CONNECTED", null, null, null, null, null));
    }

    /** "Sync now": checks the connection by fetching profile and leave types, then stamps the time. */
    @PostMapping("/sync")
    public Dtos.StatusResponse sync(HttpServletRequest req) {
        return toStatus(service.sync(users.require(req)));
    }

    private static Dtos.StatusResponse toStatus(ZohoConnection c) {
        return new Dtos.StatusResponse(c.getStatus() == ConnectionStatus.CONNECTED,
                c.getStatus().name(), c.getZohoEmail(), c.getDataCenter(),
                c.getConnectedAt(), c.getLastSyncAt(), c.getScopes());
    }

    /** Frontend calls this (with its normal auth header), then navigates the browser to authorizeUrl. */
    @PostMapping("/connect")
    public Map<String, String> connect(HttpServletRequest req, HttpServletResponse resp) {
        CurrentUser u = users.require(req);
        String nonce = oauth.newNonce();
        resp.addHeader(HttpHeaders.SET_COOKIE, nonceCookie(nonce, Duration.ofMinutes(10)).toString());
        return Map.of("authorizeUrl", oauth.buildAuthorizeUrl(u, nonce));
    }

    /** Zoho redirects the browser here. Identity comes from the signed state plus the nonce cookie. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = NONCE_COOKIE, required = false) String nonce) {

        String result;
        if (error != null) {
            result = "error&reason=access_denied";
        } else {
            try {
                oauth.handleCallback(code, location, state, nonce);
                result = "connected";
            } catch (ZohoExceptions.OAuthCallback e) {
                result = "error&reason=" + e.reason();
            } catch (ZohoExceptions.Api e) {
                result = "error&reason=zoho_error";
            }
        }
        URI target = URI.create(UriComponentsBuilder.fromUriString(props.frontendSettingsUrl())
                .query("zoho=" + result).build().toUriString());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(target)
                .header(HttpHeaders.SET_COOKIE, nonceCookie("", Duration.ZERO).toString())   // clear
                .build();
    }

    @DeleteMapping
    public ResponseEntity<Void> disconnect(HttpServletRequest req) {
        oauth.disconnect(users.require(req));
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie nonceCookie(String value, Duration maxAge) {
        return ResponseCookie.from(NONCE_COOKIE, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Lax")     // Lax so the cookie is sent on Zoho's top-level redirect back to us
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}

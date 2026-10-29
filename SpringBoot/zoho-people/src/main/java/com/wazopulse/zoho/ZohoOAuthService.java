package com.wazopulse.zoho;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ZohoOAuthService {
    /** Always start authorization at accounts.zoho.com; Zoho tells us the user's data center. */
    private static final String AUTH_URL = "https://accounts.zoho.com/oauth/v2/auth";

    private record CachedToken(String token, Instant expiresAt) {}

    private final ZohoProperties props;
    private final ZohoHttp http;
    private final TokenCrypto crypto;
    private final OAuthStateService states;
    private final ZohoConnectionRepository repo;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<Long, CachedToken> cache = new ConcurrentHashMap<>();
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    public ZohoOAuthService(ZohoProperties props, ZohoHttp http, TokenCrypto crypto,
                            OAuthStateService states, ZohoConnectionRepository repo, Clock clock) {
        this.props = props;
        this.http = http;
        this.crypto = crypto;
        this.states = states;
        this.repo = repo;
        this.clock = clock;
    }

    // ---- connect -------------------------------------------------------------------------

    public String newNonce() {
        byte[] b = new byte[16];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public String buildAuthorizeUrl(CurrentUser user, String nonce) {
        return UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", props.clientId())
                .queryParam("scope", String.join(",", props.scopes()))
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("access_type", "offline")   // needed to receive a refresh token
                .queryParam("prompt", "consent")
                .queryParam("state", states.create(user, nonce))
                .build()
                .encode()
                .toUriString();
    }

    // ---- callback ------------------------------------------------------------------------

    @Transactional
    public ZohoConnection handleCallback(String code, String location, String state, String nonceCookie) {
        CurrentUser user = states.verify(state, nonceCookie);
        if (code == null || code.isBlank()) throw new ZohoExceptions.OAuthCallback("missing_code");

        DataCenter dc = DataCenter.fromLocation(location);   // allow-listed, never taken from the request URL
        JsonNode tok = exchangeCode(dc, code);
        String accessToken = tok.path("access_token").asText(null);
        String refreshToken = tok.path("refresh_token").asText(null);
        if (accessToken == null || refreshToken == null) {
            throw new ZohoExceptions.OAuthCallback("no_refresh_token");
        }

        // The Zoho account must belong to the same person as the Wazo Pulse user.
        ZohoHttp.Raw raw = http.get(dc.peopleBase(), accessToken, "/people/api/forms/employee/getRecords",
                Map.of("searchColumn", "EMPLOYEEMAILALIAS", "searchValue", user.email()));
        JsonNode body = ZohoHttp.parse(raw.body());
        if (raw.status() >= 400 || body == null || !JsonUtil.containsText(body, user.email())) {
            revokeQuietly(dc.accountsServer(), refreshToken);
            throw new ZohoExceptions.OAuthCallback("account_mismatch");
        }
        // Field name is "Zoho_ID" in Zoho People employee records; confirm against a real response.
        String recordId = JsonUtil.findText(body, "Zoho_ID", "recordId");

        ZohoConnection c = repo.findByTenantIdAndUserId(user.tenantId(), user.userId())
                .orElseGet(ZohoConnection::new);
        c.setTenantId(user.tenantId());
        c.setUserId(user.userId());
        c.setRefreshTokenEnc(crypto.encrypt(refreshToken));
        c.setAccountsServer(dc.accountsServer());
        c.setPeopleDomain(dc.peopleBase());
        c.setDataCenter(dc.code());
        c.setZohoEmail(user.email());
        c.setZohoRecordId(recordId);
        c.setScopes(tok.path("scope").asText(String.join(",", props.scopes())));
        c.setStatus(ConnectionStatus.CONNECTED);
        c.setConnectedAt(clock.instant());
        ZohoConnection saved = repo.save(c);

        cache.put(saved.getId(), new CachedToken(accessToken, clock.instant().plusSeconds(ttl(tok))));
        return saved;
    }

    private JsonNode exchangeCode(DataCenter dc, String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", props.clientId());
        form.put("client_secret", props.clientSecret());
        form.put("redirect_uri", props.redirectUri());
        form.put("code", code);
        ZohoHttp.Raw raw = http.postForm(dc.accountsServer(), null, "/oauth/v2/token", form);
        JsonNode n = ZohoHttp.parse(raw.body());
        if (n == null || n.hasNonNull("error")) {
            throw new ZohoExceptions.OAuthCallback("token_exchange_failed");
        }
        return n;
    }

    // ---- access tokens -------------------------------------------------------------------

    /** Returns a valid access token, refreshing it when it is missing, near expiry, or forced. */
    public String accessToken(ZohoConnection c, boolean forceRefresh) {
        if (!forceRefresh && validCached(c.getId()) != null) return validCached(c.getId());
        synchronized (locks.computeIfAbsent(c.getId(), k -> new Object())) {
            if (!forceRefresh && validCached(c.getId()) != null) return validCached(c.getId());
            return refresh(c);
        }
    }

    private String validCached(Long id) {
        CachedToken t = cache.get(id);
        return t != null && t.expiresAt().isAfter(clock.instant().plusSeconds(60)) ? t.token() : null;
    }

    private String refresh(ZohoConnection c) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", props.clientId());
        form.put("client_secret", props.clientSecret());
        form.put("refresh_token", crypto.decrypt(c.getRefreshTokenEnc()));
        ZohoHttp.Raw raw = http.postForm(c.getAccountsServer(), null, "/oauth/v2/token", form);
        JsonNode n = ZohoHttp.parse(raw.body());
        if (n == null) throw new ZohoExceptions.Api(502, "Unexpected response from Zoho token endpoint");
        if (n.hasNonNull("error")) {
            String err = n.get("error").asText();
            if (err.equals("invalid_code") || err.equals("invalid_token")) {
                markNeedsReconnect(c);
                throw new ZohoExceptions.NotConnected(
                        "Your Zoho connection was revoked or expired. Please reconnect.", true);
            }
            throw new ZohoExceptions.Api(502, "Zoho token refresh failed: " + err);
        }
        String at = n.path("access_token").asText(null);
        if (at == null) throw new ZohoExceptions.Api(502, "Zoho did not return an access token");
        cache.put(c.getId(), new CachedToken(at, clock.instant().plusSeconds(ttl(n))));
        return at;
    }

    private static long ttl(JsonNode n) {
        long v = n.path("expires_in").asLong(3600);
        return v > 86_400 ? v / 1000 : v;   // some Zoho responses report milliseconds
    }

    private void markNeedsReconnect(ZohoConnection c) {
        c.setStatus(ConnectionStatus.NEEDS_RECONNECT);
        repo.save(c);
        cache.remove(c.getId());
    }

    // ---- disconnect ----------------------------------------------------------------------

    @Transactional
    public void disconnect(CurrentUser user) {
        Optional<ZohoConnection> found = repo.findByTenantIdAndUserId(user.tenantId(), user.userId());
        if (found.isEmpty()) return;
        ZohoConnection c = found.get();
        try {
            revokeQuietly(c.getAccountsServer(), crypto.decrypt(c.getRefreshTokenEnc()));
        } catch (RuntimeException ignored) {
            // still delete locally even if revoke fails
        }
        cache.remove(c.getId());
        repo.delete(c);
    }

    private void revokeQuietly(String accountsServer, String refreshToken) {
        try {
            String path = "/oauth/v2/token/revoke?token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
            http.postForm(accountsServer, null, path, Map.of());
        } catch (RuntimeException ignored) {
            // best effort
        }
    }
}

package com.wazopulse.zoho;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ZohoPeopleClient {
    private final ZohoHttp http;
    private final ZohoOAuthService oauth;
    private final ZohoProperties props;
    private final Clock clock;
    private final Map<Long, SlidingWindowLimiter> limiters = new ConcurrentHashMap<>();

    public ZohoPeopleClient(ZohoHttp http, ZohoOAuthService oauth, ZohoProperties props, Clock clock) {
        this.http = http;
        this.oauth = oauth;
        this.props = props;
        this.clock = clock;
    }

    public JsonNode get(ZohoConnection c, String path, Map<String, String> params) {
        return call(c, false, path, params);
    }

    public JsonNode postForm(ZohoConnection c, String path, Map<String, String> form) {
        return call(c, true, path, form);
    }

    private JsonNode call(ZohoConnection c, boolean post, String path, Map<String, String> data) {
        limiters.computeIfAbsent(c.getId(), k -> new SlidingWindowLimiter(props.maxRequestsPerMinute()))
                .acquire(clock.millis());

        for (int attempt = 0; attempt < 2; attempt++) {
            String token = oauth.accessToken(c, attempt == 1);
            ZohoHttp.Raw raw = post
                    ? http.postForm(c.getPeopleDomain(), token, path, data)
                    : http.get(c.getPeopleDomain(), token, path, data);
            if (attempt == 0 && isAuthFailure(raw)) continue;   // refresh once and retry
            return validate(raw);
        }
        throw new ZohoExceptions.Api(401, "Zoho rejected the access token");
    }

    private static boolean isAuthFailure(ZohoHttp.Raw raw) {
        String b = raw.body() == null ? "" : raw.body();
        return raw.status() == 401 || b.contains("INVALID_OAUTHTOKEN") || b.contains("Invalid Ticket Id");
    }

    /** Zoho often returns HTTP 200 with response.status != 0 for errors. */
    private static JsonNode validate(ZohoHttp.Raw raw) {
        JsonNode n = ZohoHttp.parse(raw.body());
        if (raw.status() >= 400) {
            throw new ZohoExceptions.Api(raw.status(), message(n, raw.body()));
        }
        if (n == null) throw new ZohoExceptions.Api(502, "Unexpected response from Zoho");
        JsonNode resp = n.path("response");
        if (resp.isObject() && resp.path("status").asInt(0) != 0) {
            throw new ZohoExceptions.Api(422, message(n, raw.body()));
        }
        return n;
    }

    private static String message(JsonNode n, String fallbackBody) {
        if (n != null) {
            JsonNode errors = n.path("response").path("errors");
            if (!errors.isMissingNode() && !errors.isNull()) return "Zoho error: " + errors;
            String m = n.path("response").path("message").asText(null);
            if (m != null) return "Zoho error: " + m;
        }
        String fb = fallbackBody == null ? "" : fallbackBody;
        return "Zoho error: " + (fb.length() > 200 ? fb.substring(0, 200) : fb);
    }
}

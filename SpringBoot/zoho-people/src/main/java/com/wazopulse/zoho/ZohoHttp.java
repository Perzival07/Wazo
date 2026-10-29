package com.wazopulse.zoho;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class ZohoHttp {
    public record Raw(int status, String body) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RestClient rest;

    public ZohoHttp(RestClient.Builder builder) {
        this.rest = builder.build();
    }

    public Raw get(String base, String accessToken, String path, Map<String, String> params) {
        String query = params == null || params.isEmpty() ? "" : "?" + params.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
        URI uri = URI.create(base + path + query);
        return rest.get().uri(uri)
                .headers(h -> { if (accessToken != null) h.set("Authorization", "Zoho-oauthtoken " + accessToken); })
                .exchange((req, res) -> new Raw(res.getStatusCode().value(), read(res.getBody().readAllBytes())));
    }

    public Raw postForm(String base, String accessToken, String path, Map<String, String> form) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (form != null) form.forEach(body::add);
        return rest.post().uri(URI.create(base + path))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(h -> { if (accessToken != null) h.set("Authorization", "Zoho-oauthtoken " + accessToken); })
                .body(body)
                .exchange((req, res) -> new Raw(res.getStatusCode().value(), read(res.getBody().readAllBytes())));
    }

    /** Returns null when the body is not JSON (Zoho sometimes returns plain-text errors). */
    public static JsonNode parse(String body) {
        try {
            return body == null || body.isBlank() ? null : MAPPER.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String read(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

package com.wazopulse.zoho;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoho")
public record ZohoProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String tokenEncryptionKey,
        String stateSigningKey,
        String frontendSettingsUrl,
        List<String> scopes,
        Integer maxRequestsPerMinute,
        String defaultZone,
        Boolean cookieSecure) {

    public ZohoProperties {
        if (scopes == null || scopes.isEmpty()) {
            scopes = List.of(
                    "ZOHOPEOPLE.forms.ALL",
                    "ZOHOPEOPLE.leave.ALL",
                    "ZOHOPEOPLE.attendance.ALL",
                    "ZOHOPEOPLE.employee.ALL");
        }
        if (maxRequestsPerMinute == null || maxRequestsPerMinute <= 0) maxRequestsPerMinute = 80;
        if (defaultZone == null || defaultZone.isBlank()) defaultZone = "Asia/Kolkata";
        if (cookieSecure == null) cookieSecure = true;
    }
}

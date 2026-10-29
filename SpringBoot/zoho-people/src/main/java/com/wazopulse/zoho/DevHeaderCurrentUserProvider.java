package com.wazopulse.zoho;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Local testing only (spring.profiles.active=dev). Never enable in production. */
@Component
@Profile("dev")
public class DevHeaderCurrentUserProvider implements CurrentUserProvider {
    @Override
    public CurrentUser require(HttpServletRequest r) {
        String tenant = r.getHeader("X-Tenant-Id");
        String user = r.getHeader("X-User-Id");
        String email = r.getHeader("X-User-Email");
        if (tenant == null || user == null || email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing dev auth headers");
        }
        return new CurrentUser(tenant, user, email);
    }
}

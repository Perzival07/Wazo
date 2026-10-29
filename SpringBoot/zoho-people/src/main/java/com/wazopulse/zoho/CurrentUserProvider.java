package com.wazopulse.zoho;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Wazo Pulse implements this and exposes it as a Spring bean, resolving the user
 * from its own auth (JWT, session, etc.).
 */
public interface CurrentUserProvider {
    CurrentUser require(HttpServletRequest request);
}

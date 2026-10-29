package com.wazopulse.zoho;

/** The logged-in Wazo Pulse user. The email must match the user's Zoho People email. */
public record CurrentUser(String tenantId, String userId, String email) {}

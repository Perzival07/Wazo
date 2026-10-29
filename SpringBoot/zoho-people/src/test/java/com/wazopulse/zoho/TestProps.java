package com.wazopulse.zoho;

import java.util.Base64;

final class TestProps {
    private TestProps() {}

    static ZohoProperties props() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);   // test-only key
        return new ZohoProperties("client-id", "client-secret", "https://app.test/cb",
                key, "state-signing-secret", "https://app.test/settings", null, null, null, null);
    }
}

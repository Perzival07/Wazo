package com.wazopulse.zoho;

public final class ZohoExceptions {
    private ZohoExceptions() {}

    /** The user has no working Zoho connection (never connected, or access was revoked). */
    public static class NotConnected extends RuntimeException {
        private final boolean reconnectRequired;
        public NotConnected(String message, boolean reconnectRequired) {
            super(message);
            this.reconnectRequired = reconnectRequired;
        }
        public boolean reconnectRequired() { return reconnectRequired; }
    }

    /** Problem during the OAuth callback. The reason is a short code safe to show in a redirect. */
    public static class OAuthCallback extends RuntimeException {
        private final String reason;
        public OAuthCallback(String reason) {
            super(reason);
            this.reason = reason;
        }
        public String reason() { return reason; }
    }

    /** Zoho returned an error. */
    public static class Api extends RuntimeException {
        private final int status;
        public Api(int status, String message) {
            super(message);
            this.status = status;
        }
        public int status() { return status; }
    }

    /** Our own per-user limiter tripped before calling Zoho. */
    public static class RateLimited extends RuntimeException {
        private final long retryAfterSeconds;
        public RateLimited(long retryAfterSeconds) {
            super("Too many Zoho requests. Try again shortly.");
            this.retryAfterSeconds = retryAfterSeconds;
        }
        public long retryAfterSeconds() { return retryAfterSeconds; }
    }
}

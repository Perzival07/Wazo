package com.wazopulse.zoho;

import java.util.ArrayDeque;
import java.util.Deque;

/** Simple per-connection limiter so one user's burst can't trigger Zoho's lock period. */
public class SlidingWindowLimiter {
    private final int max;
    private final long windowMillis = 60_000;
    private final Deque<Long> stamps = new ArrayDeque<>();

    public SlidingWindowLimiter(int maxPerMinute) {
        this.max = maxPerMinute;
    }

    public synchronized void acquire(long nowMillis) {
        while (!stamps.isEmpty() && nowMillis - stamps.peekFirst() >= windowMillis) {
            stamps.pollFirst();
        }
        if (stamps.size() >= max) {
            long waitMs = windowMillis - (nowMillis - stamps.peekFirst());
            throw new ZohoExceptions.RateLimited(Math.max(1, (waitMs + 999) / 1000));
        }
        stamps.addLast(nowMillis);
    }
}

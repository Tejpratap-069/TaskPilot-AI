package com.taskpilot.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force guard for the login endpoint. After
 * {@value #MAX_FAILURES} failed attempts for the same email inside a
 * {@value #WINDOW_MINUTES}-minute window, further attempts are rejected
 * (HTTP 429) for a {@value #COOLDOWN_MINUTES}-minute cooldown. A successful
 * login clears the counter. Dependency-free by design; state is per-instance,
 * which matches the single-service deployment.
 */
@Service
public class LoginRateLimiter {
    static final int MAX_FAILURES = 5;
    static final long WINDOW_MINUTES = 10;
    static final long COOLDOWN_MINUTES = 10;

    private static final class Attempts {
        int failures;
        Instant windowStart;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Attempts a = attemptsByEmail.get(email);
        if (a == null) return false;
        synchronized (a) {
            return a.lockedUntil != null && Instant.now().isBefore(a.lockedUntil);
        }
    }

    public void recordFailure(String email) {
        Attempts a = attemptsByEmail.computeIfAbsent(email, k -> new Attempts());
        synchronized (a) {
            Instant now = Instant.now();
            if (a.lockedUntil != null && now.isBefore(a.lockedUntil)) {
                return; // already locked; don't extend the lock on every probe
            }
            if (a.windowStart == null
                    || Duration.between(a.windowStart, now).toMinutes() >= WINDOW_MINUTES) {
                a.failures = 0;
                a.windowStart = now;
                a.lockedUntil = null;
            }
            a.failures++;
            if (a.failures >= MAX_FAILURES) {
                a.lockedUntil = now.plus(Duration.ofMinutes(COOLDOWN_MINUTES));
            }
        }
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(email);
    }
}

package com.limitr.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitDecision check(String principalId, int limitPerMinute) {
        Instant now = Instant.now();
        long currentEpochMinute = now.getEpochSecond() / 60;
        long retryAfter = Math.max(1, ((currentEpochMinute + 1) * 60) - now.getEpochSecond());

        WindowCounter counter = counters.computeIfAbsent(principalId, ignored -> new WindowCounter(currentEpochMinute, 0));
        synchronized (counter) {
            if (counter.epochMinute != currentEpochMinute) {
                counter.epochMinute = currentEpochMinute;
                counter.count = 0;
            }
            if (counter.count >= limitPerMinute) {
                return new RateLimitDecision(false, limitPerMinute, 0, retryAfter);
            }
            counter.count++;
            int remaining = Math.max(0, limitPerMinute - counter.count);
            return new RateLimitDecision(true, limitPerMinute, remaining, retryAfter);
        }
    }

    public record RateLimitDecision(
        boolean allowed,
        int limit,
        int remaining,
        long retryAfterSeconds
    ) {}

    private static final class WindowCounter {
        private long epochMinute;
        private int count;

        private WindowCounter(long epochMinute, int count) {
            this.epochMinute = epochMinute;
            this.count = count;
        }
    }
}

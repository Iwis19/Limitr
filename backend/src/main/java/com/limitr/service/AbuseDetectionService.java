package com.limitr.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Service;

@Service
public class AbuseDetectionService {

    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final Duration SPIKE_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration ENUMERATION_COOLDOWN = Duration.ofSeconds(30);

    private final Map<String, Deque<Instant>> rateLimitExceeds = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> failedAuthAttempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> sequentialEnumerations = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> trafficSpikes = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> recentRequests = new ConcurrentHashMap<>();

    private final Map<String, Long> lastResourceId = new ConcurrentHashMap<>();
    private final Map<String, Integer> resourceStreak = new ConcurrentHashMap<>();

    public void recordRateLimitExceeded(String principalId) {
        appendEvent(rateLimitExceeds, principalId, Instant.now());
    }

    public void recordFailedAuthAttempt(String principalId) {
        appendEvent(failedAuthAttempts, principalId, Instant.now());
    }

    public void recordRequest(String principalId) {
        Instant now = Instant.now();
        Deque<Instant> requests = appendEvent(recentRequests, principalId, now);
        long lastThirtySeconds = requests.stream().filter(t -> t.isAfter(now.minusSeconds(30))).count();
        if (lastThirtySeconds >= 20) {
            Deque<Instant> spikes = getDeque(trafficSpikes, principalId);
            prune(spikes, now);
            Instant lastSpike = spikes.peekLast();
            if (lastSpike == null || lastSpike.isBefore(now.minus(SPIKE_COOLDOWN))) {
                spikes.addLast(now);
            }
        }
    }

    public void recordResourceAccess(String principalId, long resourceId) {
        long nowId = resourceId;
        Long previous = lastResourceId.put(principalId, nowId);
        if (previous != null && nowId == previous + 1) {
            int streak = resourceStreak.getOrDefault(principalId, 1) + 1;
            resourceStreak.put(principalId, streak);
            if (streak >= 4) {
                Instant now = Instant.now();
                Deque<Instant> events = getDeque(sequentialEnumerations, principalId);
                prune(events, now);
                Instant last = events.peekLast();
                if (last == null || last.isBefore(now.minus(ENUMERATION_COOLDOWN))) {
                    events.addLast(now);
                }
            }
        } else {
            resourceStreak.put(principalId, 1);
        }
    }

    public int getScore(String principalId) {
        Instant now = Instant.now();
        int score = 0;
        if (!prunedEvents(rateLimitExceeds, principalId, now).isEmpty()) {
            score += 3;
        }
        if (prunedEvents(failedAuthAttempts, principalId, now).size() > 10) {
            score += 2;
        }
        if (!prunedEvents(sequentialEnumerations, principalId, now).isEmpty()) {
            score += 2;
        }
        if (!prunedEvents(trafficSpikes, principalId, now).isEmpty()) {
            score += 1;
        }
        return score;
    }

    private Deque<Instant> appendEvent(Map<String, Deque<Instant>> map, String principalId, Instant timestamp) {
        Deque<Instant> deque = getDeque(map, principalId);
        deque.addLast(timestamp);
        prune(deque, timestamp);
        return deque;
    }

    private Deque<Instant> prunedEvents(Map<String, Deque<Instant>> map, String principalId, Instant now) {
        Deque<Instant> deque = getDeque(map, principalId);
        prune(deque, now);
        return deque;
    }

    private Deque<Instant> getDeque(Map<String, Deque<Instant>> map, String principalId) {
        return map.computeIfAbsent(principalId, ignored -> new ConcurrentLinkedDeque<>());
    }

    private void prune(Deque<Instant> deque, Instant now) {
        Instant threshold = now.minus(WINDOW);
        while (!deque.isEmpty() && deque.peekFirst() != null && deque.peekFirst().isBefore(threshold)) {
            deque.pollFirst();
        }
    }
}

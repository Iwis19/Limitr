package com.limitr.service;

import com.limitr.domain.RateLimitWindow;
import com.limitr.repository.RateLimitWindowRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitService {

    private final RateLimitWindowRepository rateLimitWindowRepository;

    public RateLimitService(RateLimitWindowRepository rateLimitWindowRepository) {
        this.rateLimitWindowRepository = rateLimitWindowRepository;
    }

    @Transactional
    public RateLimitDecision check(String principalId, int limitPerMinute) {
        Instant now = Instant.now();
        long currentEpochMinute = now.getEpochSecond() / 60;
        long retryAfter = Math.max(1, ((currentEpochMinute + 1) * 60) - now.getEpochSecond());

        RateLimitWindow window = rateLimitWindowRepository
            .findByPrincipalIdAndEpochMinute(principalId, currentEpochMinute)
            .orElseGet(() -> newWindow(principalId, currentEpochMinute));

        int count = window.getRequestCount() == null ? 0 : window.getRequestCount();
        if (count >= limitPerMinute) {
            return new RateLimitDecision(false, limitPerMinute, 0, retryAfter);
        }

        int updatedCount = count + 1;
        window.setRequestCount(updatedCount);
        rateLimitWindowRepository.save(window);

        int remaining = Math.max(0, limitPerMinute - updatedCount);
        return new RateLimitDecision(true, limitPerMinute, remaining, retryAfter);
    }

    private RateLimitWindow newWindow(String principalId, long epochMinute) {
        RateLimitWindow window = new RateLimitWindow();
        window.setPrincipalId(principalId);
        window.setEpochMinute(epochMinute);
        window.setRequestCount(0);
        return window;
    }

    public record RateLimitDecision(
        boolean allowed,
        int limit,
        int remaining,
        long retryAfterSeconds
    ) {}
}

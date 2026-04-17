package com.limitr.service;

import com.limitr.domain.AbuseSignal;
import com.limitr.repository.AbuseSignalRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbuseDetectionService {

    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final Duration SPIKE_WINDOW = Duration.ofSeconds(30);
    private static final Duration SPIKE_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration ENUMERATION_COOLDOWN = Duration.ofSeconds(30);

    private static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    private static final String FAILED_AUTH = "FAILED_AUTH";
    private static final String RESOURCE_ACCESS = "RESOURCE_ACCESS";
    private static final String SEQUENTIAL_ENUMERATION = "SEQUENTIAL_ENUMERATION";
    private static final String REQUEST = "REQUEST";
    private static final String TRAFFIC_SPIKE = "TRAFFIC_SPIKE";

    private final AbuseSignalRepository abuseSignalRepository;

    public AbuseDetectionService(AbuseSignalRepository abuseSignalRepository) {
        this.abuseSignalRepository = abuseSignalRepository;
    }

    @Transactional
    public void recordRateLimitExceeded(String principalId) {
        saveSignal(principalId, RATE_LIMIT_EXCEEDED, null, Instant.now());
    }

    @Transactional
    public void recordFailedAuthAttempt(String principalId) {
        saveSignal(principalId, FAILED_AUTH, null, Instant.now());
    }

    @Transactional
    public void recordRequest(String principalId) {
        Instant now = Instant.now();
        saveSignal(principalId, REQUEST, null, now);

        long recentRequests = abuseSignalRepository.countByPrincipalIdAndSignalTypeAndTimestampAfter(
            principalId,
            REQUEST,
            now.minus(SPIKE_WINDOW)
        );
        boolean recentlyRecordedSpike = abuseSignalRepository.existsByPrincipalIdAndSignalTypeAndTimestampAfter(
            principalId,
            TRAFFIC_SPIKE,
            now.minus(SPIKE_COOLDOWN)
        );

        if (recentRequests >= 20 && !recentlyRecordedSpike) {
            saveSignal(principalId, TRAFFIC_SPIKE, null, now);
        }
    }

    @Transactional
    public void recordResourceAccess(String principalId, long resourceId) {
        Instant now = Instant.now();
        saveSignal(principalId, RESOURCE_ACCESS, resourceId, now);

        if (hasSequentialResourceStreak(principalId)
            && !abuseSignalRepository.existsByPrincipalIdAndSignalTypeAndTimestampAfter(
                principalId,
                SEQUENTIAL_ENUMERATION,
                now.minus(ENUMERATION_COOLDOWN)
            )) {
            saveSignal(principalId, SEQUENTIAL_ENUMERATION, null, now);
        }
    }

    @Transactional(readOnly = true)
    public int getScore(String principalId) {
        Instant threshold = Instant.now().minus(WINDOW);
        int score = 0;

        if (abuseSignalRepository.existsByPrincipalIdAndSignalTypeAndTimestampAfter(
            principalId,
            RATE_LIMIT_EXCEEDED,
            threshold
        )) {
            score += 3;
        }

        if (abuseSignalRepository.countByPrincipalIdAndSignalTypeAndTimestampAfter(
            principalId,
            FAILED_AUTH,
            threshold
        ) > 10) {
            score += 2;
        }

        if (abuseSignalRepository.existsByPrincipalIdAndSignalTypeAndTimestampAfter(
            principalId,
            SEQUENTIAL_ENUMERATION,
            threshold
        )) {
            score += 2;
        }

        if (abuseSignalRepository.existsByPrincipalIdAndSignalTypeAndTimestampAfter(
            principalId,
            TRAFFIC_SPIKE,
            threshold
        )) {
            score += 1;
        }

        return score;
    }

    private boolean hasSequentialResourceStreak(String principalId) {
        List<AbuseSignal> recentAccesses = abuseSignalRepository
            .findTop10ByPrincipalIdAndSignalTypeOrderByTimestampDesc(principalId, RESOURCE_ACCESS);

        Long previous = null;
        int streak = 0;
        for (AbuseSignal signal : recentAccesses) {
            Long current = signal.getResourceId();
            if (current == null) {
                break;
            }
            if (previous == null) {
                streak = 1;
            } else if (current == previous - 1) {
                streak++;
            } else {
                break;
            }
            if (streak >= 4) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    private void saveSignal(String principalId, String signalType, Long resourceId, Instant timestamp) {
        AbuseSignal signal = new AbuseSignal();
        signal.setPrincipalId(principalId);
        signal.setSignalType(signalType);
        signal.setResourceId(resourceId);
        signal.setTimestamp(timestamp);
        abuseSignalRepository.save(signal);
    }
}

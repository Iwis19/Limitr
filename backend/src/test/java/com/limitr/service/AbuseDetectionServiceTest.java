package com.limitr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.limitr.domain.AbuseSignal;
import com.limitr.repository.AbuseSignalRepository;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AbuseDetectionServiceTest {

    @Test
    void rateLimitSignalsContributeToScoreAcrossServiceInstances() {
        AbuseSignalRepositoryFixture fixture = new AbuseSignalRepositoryFixture();
        AbuseDetectionService writer = new AbuseDetectionService(fixture.repository);
        AbuseDetectionService reader = new AbuseDetectionService(fixture.repository);

        writer.recordRateLimitExceeded("client-a");

        assertEquals(3, reader.getScore("client-a"));
    }

    @Test
    void failedAuthAttemptsContributeToScoreAcrossServiceInstances() {
        AbuseSignalRepositoryFixture fixture = new AbuseSignalRepositoryFixture();
        AbuseDetectionService writer = new AbuseDetectionService(fixture.repository);
        AbuseDetectionService reader = new AbuseDetectionService(fixture.repository);

        for (int i = 0; i < 11; i++) {
            writer.recordFailedAuthAttempt("anonymous:127.0.0.1");
        }

        assertEquals(2, reader.getScore("anonymous:127.0.0.1"));
    }

    @Test
    void trafficSpikeDetectionCanSpanServiceInstances() {
        AbuseSignalRepositoryFixture fixture = new AbuseSignalRepositoryFixture();
        AbuseDetectionService writer = new AbuseDetectionService(fixture.repository);
        AbuseDetectionService reader = new AbuseDetectionService(fixture.repository);

        for (int i = 0; i < 10; i++) {
            writer.recordRequest("client-a");
        }
        for (int i = 0; i < 10; i++) {
            reader.recordRequest("client-a");
        }

        assertEquals(1, reader.getScore("client-a"));
    }

    @Test
    void resourceEnumerationDetectionCanSpanServiceInstances() {
        AbuseSignalRepositoryFixture fixture = new AbuseSignalRepositoryFixture();
        AbuseDetectionService writer = new AbuseDetectionService(fixture.repository);
        AbuseDetectionService reader = new AbuseDetectionService(fixture.repository);

        writer.recordResourceAccess("client-a", 101);
        writer.recordResourceAccess("client-a", 102);
        reader.recordResourceAccess("client-a", 103);
        reader.recordResourceAccess("client-a", 104);

        assertEquals(2, reader.getScore("client-a"));
    }

    private static class AbuseSignalRepositoryFixture {

        private final AtomicLong sequence = new AtomicLong();
        private final List<AbuseSignal> signals = new ArrayList<>();
        private final AbuseSignalRepository repository = createRepository();

        private AbuseSignalRepository createRepository() {
            return (AbuseSignalRepository) Proxy.newProxyInstance(
                AbuseSignalRepository.class.getClassLoader(),
                new Class<?>[] { AbuseSignalRepository.class },
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "save" -> {
                            AbuseSignal signal = (AbuseSignal) args[0];
                            signal.setTimestamp(Instant.now().plusMillis(sequence.incrementAndGet()));
                            signals.add(signal);
                            yield signal;
                        }
                        case "existsByPrincipalIdAndSignalTypeAndTimestampAfter" -> {
                            String principalId = (String) args[0];
                            String signalType = (String) args[1];
                            Instant timestamp = (Instant) args[2];
                            yield signals.stream().anyMatch(signal -> matches(signal, principalId, signalType, timestamp));
                        }
                        case "countByPrincipalIdAndSignalTypeAndTimestampAfter" -> {
                            String principalId = (String) args[0];
                            String signalType = (String) args[1];
                            Instant timestamp = (Instant) args[2];
                            yield signals.stream()
                                .filter(signal -> matches(signal, principalId, signalType, timestamp))
                                .count();
                        }
                        case "findTop10ByPrincipalIdAndSignalTypeOrderByTimestampDesc" -> {
                            String principalId = (String) args[0];
                            String signalType = (String) args[1];
                            yield signals.stream()
                                .filter(signal -> principalId.equals(signal.getPrincipalId()))
                                .filter(signal -> signalType.equals(signal.getSignalType()))
                                .sorted(Comparator.comparing(AbuseSignal::getTimestamp).reversed())
                                .limit(10)
                                .toList();
                        }
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "AbuseSignalRepositoryTestProxy";
                        default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                    };
                }
            );
        }

        private boolean matches(AbuseSignal signal, String principalId, String signalType, Instant after) {
            return principalId.equals(signal.getPrincipalId())
                && signalType.equals(signal.getSignalType())
                && signal.getTimestamp().isAfter(after);
        }
    }
}

package com.limitr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limitr.domain.Incident;
import com.limitr.domain.RuleConfig;
import com.limitr.domain.enums.EnforcementState;
import com.limitr.repository.IncidentRepository;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class EnforcementServiceTest {

    @Test
    void manualBanSurvivesServiceRecreation() {
        IncidentRepositoryFixture fixture = incidentRepositoryFixture();

        EnforcementService writer = new EnforcementService(null, fixture.repository);
        writer.banManually("persisted-client", 15);

        EnforcementService reader = new EnforcementService(null, fixture.repository);

        assertTrue(reader.isTempBanned("persisted-client"));
        assertEquals(1, reader.activeBanCount());
        assertEquals(Map.of("persisted-client", reader.getActiveBans().get("persisted-client")), reader.getActiveBans());
    }

    @Test
    void manualUnbanClearsPersistedBanForNewServiceInstance() {
        IncidentRepositoryFixture fixture = incidentRepositoryFixture();

        EnforcementService writer = new EnforcementService(null, fixture.repository);
        writer.banManually("persisted-client", 15);
        writer.unban("persisted-client");

        EnforcementService reader = new EnforcementService(null, fixture.repository);

        assertFalse(reader.isTempBanned("persisted-client"));
        assertEquals(0, reader.activeBanCount());
        assertEquals(Map.of(), reader.getActiveBans());
    }

    @Test
    void automaticBanSurvivesServiceRecreation() {
        IncidentRepositoryFixture fixture = incidentRepositoryFixture();
        RuleConfig ruleConfig = ruleConfig();

        EnforcementService writer = new EnforcementService(new ScoreAbuseDetectionService(7), fixture.repository);

        assertEquals(EnforcementState.TEMP_BANNED, writer.evaluate("persisted-client", ruleConfig));

        EnforcementService reader = new EnforcementService(new ScoreAbuseDetectionService(0), fixture.repository);

        assertTrue(reader.isTempBanned("persisted-client"));
        assertEquals(EnforcementState.TEMP_BANNED, reader.evaluate("persisted-client", ruleConfig));
        assertEquals(1, reader.activeBanCount());
    }

    @Test
    void incidentDedupeUsesPersistedStateAcrossServiceInstances() {
        IncidentRepositoryFixture fixture = incidentRepositoryFixture();
        RuleConfig ruleConfig = ruleConfig();

        EnforcementService writer = new EnforcementService(new ScoreAbuseDetectionService(2), fixture.repository);
        EnforcementService reader = new EnforcementService(new ScoreAbuseDetectionService(2), fixture.repository);

        assertEquals(EnforcementState.WARN, writer.evaluate("noisy-client", ruleConfig));
        assertEquals(EnforcementState.WARN, reader.evaluate("noisy-client", ruleConfig));

        assertEquals(1, fixture.incidentsFor("noisy-client").size());
    }

    private static RuleConfig ruleConfig() {
        RuleConfig ruleConfig = new RuleConfig();
        ruleConfig.setWarnThreshold(2);
        ruleConfig.setThrottleThreshold(4);
        ruleConfig.setBanThreshold(7);
        ruleConfig.setBanMinutes(15);
        return ruleConfig;
    }

    private static IncidentRepositoryFixture incidentRepositoryFixture() {
        return new IncidentRepositoryFixture();
    }

    private static class ScoreAbuseDetectionService extends AbuseDetectionService {

        private final int score;

        ScoreAbuseDetectionService(int score) {
            super(null);
            this.score = score;
        }

        @Override
        public int getScore(String principalId) {
            return score;
        }
    }

    private static class IncidentRepositoryFixture {

        private final List<Incident> incidents = new ArrayList<>();
        private final AtomicLong sequence = new AtomicLong();
        private final IncidentRepository repository = createRepository();

        private List<Incident> incidentsFor(String principalId) {
            return incidents.stream()
                .filter(incident -> principalId.equals(incident.getPrincipalId()))
                .toList();
        }

        private IncidentRepository createRepository() {
            return (IncidentRepository) Proxy.newProxyInstance(
                IncidentRepository.class.getClassLoader(),
                new Class<?>[] { IncidentRepository.class },
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "save" -> {
                            Incident incident = (Incident) args[0];
                            if (incident.getTimestamp() == null) {
                                incident.setTimestamp(Instant.now().plusMillis(sequence.incrementAndGet()));
                            }
                            incidents.add(incident);
                            yield incident;
                        }
                        case "findTopByPrincipalIdOrderByTimestampDesc" -> {
                            String principalId = (String) args[0];
                            yield incidents.stream()
                                .filter(incident -> principalId.equals(incident.getPrincipalId()))
                                .max(Comparator.comparing(Incident::getTimestamp));
                        }
                        case "findActiveBanStates" -> {
                            Instant now = (Instant) args[0];
                            Map<String, Incident> latestByPrincipal = new LinkedHashMap<>();
                            incidents.stream()
                                .sorted(Comparator.comparing(Incident::getTimestamp))
                                .forEach(incident -> latestByPrincipal.put(incident.getPrincipalId(), incident));

                            yield latestByPrincipal.values().stream()
                                .filter(incident -> "TEMP_BANNED".equals(incident.getActionTaken()))
                                .filter(incident -> incident.getExpiresAt() != null && incident.getExpiresAt().isAfter(now))
                                .toList();
                        }
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "IncidentRepositoryEnforcementTestProxy";
                        default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                    };
                }
            );
        }
    }
}

package com.limitr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limitr.domain.Incident;
import com.limitr.repository.IncidentRepository;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class EnforcementServiceTest {

    @Test
    void manualBanSurvivesServiceRecreation() {
        IncidentRepository incidentRepository = incidentRepositoryStub();

        EnforcementService writer = new EnforcementService(null, incidentRepository);
        writer.banManually("persisted-client", 15);

        EnforcementService reader = new EnforcementService(null, incidentRepository);

        assertTrue(reader.isTempBanned("persisted-client"));
        assertEquals(1, reader.activeBanCount());
        assertEquals(Map.of("persisted-client", reader.getActiveBans().get("persisted-client")), reader.getActiveBans());
    }

    @Test
    void manualUnbanClearsPersistedBanForNewServiceInstance() {
        IncidentRepository incidentRepository = incidentRepositoryStub();

        EnforcementService writer = new EnforcementService(null, incidentRepository);
        writer.banManually("persisted-client", 15);
        writer.unban("persisted-client");

        EnforcementService reader = new EnforcementService(null, incidentRepository);

        assertFalse(reader.isTempBanned("persisted-client"));
        assertEquals(0, reader.activeBanCount());
        assertEquals(Map.of(), reader.getActiveBans());
    }

    private static IncidentRepository incidentRepositoryStub() {
        List<Incident> incidents = new ArrayList<>();
        AtomicLong sequence = new AtomicLong();

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

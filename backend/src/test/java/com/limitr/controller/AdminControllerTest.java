package com.limitr.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limitr.domain.Incident;
import com.limitr.domain.RuleConfig;
import com.limitr.repository.IncidentRepository;
import com.limitr.service.EnforcementService;
import com.limitr.service.RequestLogService;
import com.limitr.service.RuleService;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AdminControllerTest {

    @Test
    void statsReturnsOperationalStatusWhenNoBansAndNoServerErrors() {
        AdminController controller = new AdminController(
            requestLogServiceStub(12, 0),
            incidentRepositoryWithTopOffenders(List.of()),
            ruleServiceStub(),
            enforcementServiceStub(0)
        );

        Map<String, Object> response = controller.stats();

        assertSystemStatus(response, "operational", "System Operational");
    }

    @Test
    void statsReturnsWarningStatusWhenProtectiveActionsAreActive() {
        AdminController controller = new AdminController(
            requestLogServiceStub(12, 0),
            incidentRepositoryWithTopOffenders(List.of()),
            ruleServiceStub(),
            enforcementServiceStub(2)
        );

        Map<String, Object> response = controller.stats();

        assertSystemStatus(response, "warning", "Protective Actions Active");
    }

    @Test
    void statsReturnsDegradedStatusWhenServerErrorsSpike() {
        AdminController controller = new AdminController(
            requestLogServiceStub(12, 3),
            incidentRepositoryWithTopOffenders(List.of()),
            ruleServiceStub(),
            enforcementServiceStub(0)
        );

        Map<String, Object> response = controller.stats();

        assertSystemStatus(response, "degraded", "System Degraded");
    }

    @Test
    void activeOnlyReturnsEmptyWhenThereAreNoPersistedActiveBans() {
        AtomicReference<Boolean> queriedActiveIncidents = new AtomicReference<>(false);
        IncidentRepository incidentRepository = incidentRepositoryProxy(queriedActiveIncidents, List.of());
        AdminController controller = new AdminController(null, incidentRepository, null, null);

        Map<String, Object> response = controller.incidents(true);

        assertEquals(List.of(), response.get("items"));
        assertTrue(queriedActiveIncidents.get(), "incident repository should be queried for persisted active bans");
    }

    @Test
    void activeOnlyReturnsPersistedActiveBanStates() {
        Incident incident = new Incident();
        incident.setPrincipalId("still-banned");

        AtomicReference<Boolean> queriedActiveIncidents = new AtomicReference<>(false);
        IncidentRepository incidentRepository = incidentRepositoryProxy(queriedActiveIncidents, List.of(incident));
        AdminController controller = new AdminController(null, incidentRepository, null, null);

        Map<String, Object> response = controller.incidents(true);

        assertEquals(List.of(incident), response.get("items"));
        assertTrue(queriedActiveIncidents.get(), "incident repository should be queried for active bans");
    }

    private static void assertSystemStatus(Map<String, Object> response, String expectedLevel, String expectedLabel) {
        Map<?, ?> status = (Map<?, ?>) response.get("systemStatus");
        assertEquals(expectedLevel, status.get("level"));
        assertEquals(expectedLabel, status.get("label"));
    }

    private static RequestLogService requestLogServiceStub(long requestsPerMinute, long serverErrorsLastHour) {
        return new RequestLogService(null) {
            @Override
            public long requestsPerMinute() {
                return requestsPerMinute;
            }

            @Override
            public long serverErrorsLastHour() {
                return serverErrorsLastHour;
            }
        };
    }

    private static RuleService ruleServiceStub() {
        return new RuleService(null) {
            @Override
            public RuleConfig getCurrentRule() {
                RuleConfig ruleConfig = new RuleConfig();
                ruleConfig.setBaseLimitPerMinute(60);
                ruleConfig.setThrottledLimitPerMinute(20);
                ruleConfig.setWarnThreshold(2);
                ruleConfig.setThrottleThreshold(4);
                ruleConfig.setBanThreshold(7);
                ruleConfig.setBanMinutes(15);
                return ruleConfig;
            }
        };
    }

    private static EnforcementService enforcementServiceStub(long activeBanCount) {
        return new EnforcementService(null, null) {
            @Override
            public long activeBanCount() {
                return activeBanCount;
            }
        };
    }

    private static IncidentRepository incidentRepositoryWithTopOffenders(List<IncidentRepository.TopOffenderProjection> topOffenders) {
        return (IncidentRepository) Proxy.newProxyInstance(
            IncidentRepository.class.getClassLoader(),
            new Class<?>[] { IncidentRepository.class },
            (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "findTopOffenders" -> topOffenders;
                    case "findActiveBanStates" -> List.of();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "IncidentRepositoryStatsProxy";
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                };
            }
        );
    }

    private static IncidentRepository incidentRepositoryProxy(
        AtomicReference<Boolean> queriedActiveIncidents,
        List<Incident> incidents
    ) {
        return (IncidentRepository) Proxy.newProxyInstance(
            IncidentRepository.class.getClassLoader(),
            new Class<?>[] { IncidentRepository.class },
            (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "findActiveBanStates" -> {
                        queriedActiveIncidents.set(true);
                        yield incidents;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "IncidentRepositoryTestProxy";
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                };
            }
        );
    }
}

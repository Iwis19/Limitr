package com.limitr.service;

import com.limitr.domain.Incident;
import com.limitr.domain.RuleConfig;
import com.limitr.domain.enums.EnforcementState;
import com.limitr.repository.IncidentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class EnforcementService {

    private static final Duration INCIDENT_DEDUP_WINDOW = Duration.ofMinutes(1);

    private final AbuseDetectionService abuseDetectionService;
    private final IncidentRepository incidentRepository;

    private final Map<String, Instant> temporaryBans = new ConcurrentHashMap<>();
    private final Map<String, EnforcementState> lastState = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastIncidentTime = new ConcurrentHashMap<>();

    public EnforcementService(AbuseDetectionService abuseDetectionService, IncidentRepository incidentRepository) {
        this.abuseDetectionService = abuseDetectionService;
        this.incidentRepository = incidentRepository;
    }

    public EnforcementState evaluate(String principalId, RuleConfig ruleConfig) {
        clearExpiredBan(principalId);
        if (isTempBanned(principalId)) {
            return EnforcementState.TEMP_BANNED;
        }

        int score = abuseDetectionService.getScore(principalId);
        EnforcementState state = EnforcementState.OK;

        if (score >= ruleConfig.getBanThreshold()) {
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(ruleConfig.getBanMinutes()));
            temporaryBans.put(principalId, expiresAt);
            state = EnforcementState.TEMP_BANNED;
            logIncident(principalId, "BAN_THRESHOLD", score, "TEMP_BANNED", expiresAt, state);
            return state;
        }
        if (score >= ruleConfig.getThrottleThreshold()) {
            state = EnforcementState.THROTTLED;
            logIncident(principalId, "THROTTLE_THRESHOLD", score, "THROTTLED", null, state);
            return state;
        }
        if (score >= ruleConfig.getWarnThreshold()) {
            state = EnforcementState.WARN;
            logIncident(principalId, "WARN_THRESHOLD", score, "WARN", null, state);
            return state;
        }

        lastState.put(principalId, EnforcementState.OK);
        return state;
    }

    public boolean isTempBanned(String principalId) {
        Instant expiresAt = temporaryBans.get(principalId);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    public void recordRateLimitIncident(String principalId, int score) {
        logIncident(principalId, "RATE_LIMIT_EXCEEDED", score, "WARN", null, EnforcementState.WARN);
    }

    public void banManually(String principalId, int minutes) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(minutes));
        temporaryBans.put(principalId, expiresAt);
        Incident incident = new Incident();
        incident.setPrincipalId(principalId);
        incident.setRuleTriggered("MANUAL_BAN");
        incident.setScore(0);
        incident.setActionTaken("TEMP_BANNED");
        incident.setExpiresAt(expiresAt);
        incidentRepository.save(incident);
        lastState.put(principalId, EnforcementState.TEMP_BANNED);
    }

    public void unban(String principalId) {
        temporaryBans.remove(principalId);
        Incident incident = new Incident();
        incident.setPrincipalId(principalId);
        incident.setRuleTriggered("MANUAL_UNBAN");
        incident.setScore(0);
        incident.setActionTaken("UNBANNED");
        incident.setExpiresAt(null);
        incidentRepository.save(incident);
        lastState.put(principalId, EnforcementState.OK);
    }

    public long activeBanCount() {
        Instant now = Instant.now();
        temporaryBans.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        return temporaryBans.size();
    }

    public Map<String, Instant> getActiveBans() {
        Instant now = Instant.now();
        temporaryBans.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        return new HashMap<>(temporaryBans);
    }

    private void clearExpiredBan(String principalId) {
        Instant expiresAt = temporaryBans.get(principalId);
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            temporaryBans.remove(principalId);
        }
    }

    private void logIncident(
        String principalId,
        String ruleTriggered,
        int score,
        String actionTaken,
        Instant expiresAt,
        EnforcementState currentState
    ) {
        Instant now = Instant.now();
        EnforcementState previousState = lastState.get(principalId);
        Instant previousIncident = lastIncidentTime.get(principalId);
        boolean recentlyLogged = previousIncident != null && previousIncident.isAfter(now.minus(INCIDENT_DEDUP_WINDOW));

        if (currentState == previousState && recentlyLogged) {
            return;
        }

        Incident incident = new Incident();
        incident.setPrincipalId(principalId);
        incident.setRuleTriggered(ruleTriggered);
        incident.setScore(score);
        incident.setActionTaken(actionTaken);
        incident.setExpiresAt(expiresAt);
        incidentRepository.save(incident);
        lastState.put(principalId, currentState);
        lastIncidentTime.put(principalId, now);
    }
}

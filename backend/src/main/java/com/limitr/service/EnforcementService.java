package com.limitr.service;

import com.limitr.domain.Incident;
import com.limitr.domain.RuleConfig;
import com.limitr.domain.enums.EnforcementState;
import com.limitr.repository.IncidentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EnforcementService {

    private static final Duration INCIDENT_DEDUP_WINDOW = Duration.ofMinutes(1);

    private final AbuseDetectionService abuseDetectionService;
    private final IncidentRepository incidentRepository;

    public EnforcementService(AbuseDetectionService abuseDetectionService, IncidentRepository incidentRepository) {
        this.abuseDetectionService = abuseDetectionService;
        this.incidentRepository = incidentRepository;
    }

    public EnforcementState evaluate(String principalId, RuleConfig ruleConfig) {
        if (isTempBanned(principalId)) {
            return EnforcementState.TEMP_BANNED;
        }

        int score = abuseDetectionService.getScore(principalId);
        EnforcementState state = EnforcementState.OK;

        if (score >= ruleConfig.getBanThreshold()) {
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(ruleConfig.getBanMinutes()));
            state = EnforcementState.TEMP_BANNED;
            logIncident(principalId, "BAN_THRESHOLD", score, "TEMP_BANNED", expiresAt);
            return state;
        }
        if (score >= ruleConfig.getThrottleThreshold()) {
            state = EnforcementState.THROTTLED;
            logIncident(principalId, "THROTTLE_THRESHOLD", score, "THROTTLED", null);
            return state;
        }
        if (score >= ruleConfig.getWarnThreshold()) {
            state = EnforcementState.WARN;
            logIncident(principalId, "WARN_THRESHOLD", score, "WARN", null);
            return state;
        }

        return state;
    }

    public boolean isTempBanned(String principalId) {
        return getBanExpiry(principalId).isAfter(Instant.now());
    }

    public void recordRateLimitIncident(String principalId, int score) {
        logIncident(principalId, "RATE_LIMIT_EXCEEDED", score, "WARN", null);
    }

    public void banManually(String principalId, int minutes) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(minutes));
        Incident incident = new Incident();
        incident.setPrincipalId(principalId);
        incident.setRuleTriggered("MANUAL_BAN");
        incident.setScore(0);
        incident.setActionTaken("TEMP_BANNED");
        incident.setExpiresAt(expiresAt);
        incidentRepository.save(incident);
    }

    public void unban(String principalId) {
        Incident incident = new Incident();
        incident.setPrincipalId(principalId);
        incident.setRuleTriggered("MANUAL_UNBAN");
        incident.setScore(0);
        incident.setActionTaken("UNBANNED");
        incident.setExpiresAt(null);
        incidentRepository.save(incident);
    }

    public long activeBanCount() {
        return getActiveBans().size();
    }

    public Map<String, Instant> getActiveBans() {
        Instant now = Instant.now();
        Map<String, Instant> activeBans = new LinkedHashMap<>();
        for (Incident incident : incidentRepository.findActiveBanStates(now)) {
            if (incident.getExpiresAt() != null) {
                activeBans.put(incident.getPrincipalId(), incident.getExpiresAt());
            }
        }
        return activeBans;
    }

    private Instant getBanExpiry(String principalId) {
        return incidentRepository.findTopByPrincipalIdOrderByTimestampDesc(principalId)
            .filter(incident -> "TEMP_BANNED".equals(incident.getActionTaken()))
            .map(Incident::getExpiresAt)
            .filter(expiresAt -> expiresAt != null && expiresAt.isAfter(Instant.now()))
            .orElse(Instant.EPOCH);
    }

    private void logIncident(
        String principalId,
        String ruleTriggered,
        int score,
        String actionTaken,
        Instant expiresAt
    ) {
        Instant now = Instant.now();

        if (hasRecentlyLoggedSameAction(principalId, actionTaken, now)) {
            return;
        }

        Incident incident = new Incident();
        incident.setPrincipalId(principalId);
        incident.setRuleTriggered(ruleTriggered);
        incident.setScore(score);
        incident.setActionTaken(actionTaken);
        incident.setExpiresAt(expiresAt);
        incidentRepository.save(incident);
    }

    private boolean hasRecentlyLoggedSameAction(String principalId, String actionTaken, Instant now) {
        return incidentRepository.findTopByPrincipalIdOrderByTimestampDesc(principalId)
            .filter(incident -> actionTaken.equals(incident.getActionTaken()))
            .map(Incident::getTimestamp)
            .filter(timestamp -> timestamp != null && timestamp.isAfter(now.minus(INCIDENT_DEDUP_WINDOW)))
            .isPresent();
    }
}

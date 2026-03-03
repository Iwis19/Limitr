package com.limitr.controller;

import com.limitr.domain.Incident;
import com.limitr.domain.RuleConfig;
import com.limitr.dto.ManualBanRequest;
import com.limitr.dto.RulesUpdateRequest;
import com.limitr.repository.IncidentRepository;
import com.limitr.service.EnforcementService;
import com.limitr.service.RequestLogService;
import com.limitr.service.RuleService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RequestLogService requestLogService;
    private final IncidentRepository incidentRepository;
    private final RuleService ruleService;
    private final EnforcementService enforcementService;

    public AdminController(
        RequestLogService requestLogService,
        IncidentRepository incidentRepository,
        RuleService ruleService,
        EnforcementService enforcementService
    ) {
        this.requestLogService = requestLogService;
        this.incidentRepository = incidentRepository;
        this.ruleService = ruleService;
        this.enforcementService = enforcementService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        List<Map<String, Object>> topOffenders = incidentRepository.findTopOffenders(oneHourAgo)
            .stream()
            .limit(5)
            .map(item -> Map.<String, Object>of(
                "principalId", item.getPrincipalId(),
                "incidents", item.getIncidents()
            ))
            .collect(Collectors.toList());

        return Map.of(
            "requestsPerMinute", requestLogService.requestsPerMinute(),
            "activeBans", enforcementService.activeBanCount(),
            "topOffenders", topOffenders,
            "rules", ruleService.getCurrentRule()
        );
    }

    @GetMapping("/logs")
    public Map<String, Object> logs(
        @RequestParam(required = false) String principalId,
        @RequestParam(required = false) Integer statusCode
    ) {
        return Map.of("items", requestLogService.findRecent(principalId, statusCode));
    }

    @GetMapping("/incidents")
    public Map<String, Object> incidents(@RequestParam(defaultValue = "false") boolean activeOnly) {
        List<Incident> items = activeOnly
            ? incidentRepository.findByExpiresAtAfter(Instant.now())
            : incidentRepository.findTop200ByOrderByTimestampDesc();
        return Map.of("items", items);
    }

    @PutMapping("/rules")
    public RuleConfig updateRules(@Valid @RequestBody RulesUpdateRequest request) {
        return ruleService.update(request);
    }

    @PostMapping("/actions/ban")
    public ResponseEntity<?> ban(@Valid @RequestBody ManualBanRequest request) {
        int duration = request.minutes() == null ? 15 : request.minutes();
        enforcementService.banManually(request.principalId(), duration);
        return ResponseEntity.ok(Map.of(
            "message", "Principal banned.",
            "principalId", request.principalId(),
            "minutes", duration
        ));
    }

    @PostMapping("/actions/unban")
    public ResponseEntity<?> unban(@RequestBody Map<String, String> payload) {
        String principalId = payload.get("principalId");
        if (principalId == null || principalId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "principalId is required."));
        }
        enforcementService.unban(principalId);
        return ResponseEntity.ok(Map.of(
            "message", "Principal unbanned.",
            "principalId", principalId
        ));
    }
}

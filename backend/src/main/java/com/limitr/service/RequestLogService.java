package com.limitr.service;

import com.limitr.domain.RequestLog;
import com.limitr.repository.RequestLogRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;

    public RequestLogService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional
    public void log(
        String principalId,
        String ip,
        String method,
        String path,
        int statusCode,
        long latencyMs
    ) {
        RequestLog requestLog = new RequestLog();
        requestLog.setPrincipalId(principalId);
        requestLog.setIpAddress(ip);
        requestLog.setHttpMethod(method);
        requestLog.setPath(path);
        requestLog.setStatusCode(statusCode);
        requestLog.setLatencyMs(latencyMs);
        requestLogRepository.save(requestLog);
    }

    public long requestsPerMinute() {
        return requestLogRepository.countByTimestampAfter(Instant.now().minusSeconds(60));
    }

    public long serverErrorsLastHour() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        return requestLogRepository.findTop200ByOrderByTimestampDesc().stream()
            .filter(log -> log.getTimestamp() != null && log.getTimestamp().isAfter(oneHourAgo))
            .filter(log -> log.getStatusCode() != null && log.getStatusCode() >= 500)
            .count();
    }

    public List<RequestLog> findRecent(String principalId, Integer statusCode) {
        if (principalId != null && !principalId.isBlank()) {
            return requestLogRepository.findTop200ByPrincipalIdOrderByTimestampDesc(principalId);
        }
        if (statusCode != null) {
            return requestLogRepository.findTop200ByStatusCodeOrderByTimestampDesc(statusCode);
        }
        return requestLogRepository.findTop200ByOrderByTimestampDesc();
    }
}

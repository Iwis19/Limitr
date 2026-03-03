package com.shieldgate.repository;

import com.shieldgate.domain.RequestLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    long countByTimestampAfter(Instant timestamp);
    List<RequestLog> findTop200ByOrderByTimestampDesc();
    List<RequestLog> findTop200ByPrincipalIdOrderByTimestampDesc(String principalId);
    List<RequestLog> findTop200ByStatusCodeOrderByTimestampDesc(Integer statusCode);
}

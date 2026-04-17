package com.limitr.repository;

import com.limitr.domain.AbuseSignal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbuseSignalRepository extends JpaRepository<AbuseSignal, Long> {
    boolean existsByPrincipalIdAndSignalTypeAndTimestampAfter(String principalId, String signalType, Instant timestamp);
    long countByPrincipalIdAndSignalTypeAndTimestampAfter(String principalId, String signalType, Instant timestamp);
    List<AbuseSignal> findTop10ByPrincipalIdAndSignalTypeOrderByTimestampDesc(String principalId, String signalType);
}

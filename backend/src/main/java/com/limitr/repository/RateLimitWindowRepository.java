package com.limitr.repository;

import com.limitr.domain.RateLimitWindow;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RateLimitWindowRepository extends JpaRepository<RateLimitWindow, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RateLimitWindow> findByPrincipalIdAndEpochMinute(String principalId, Long epochMinute);
}

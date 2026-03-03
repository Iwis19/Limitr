package com.limitr.repository;

import com.limitr.domain.Incident;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findTop200ByOrderByTimestampDesc();
    List<Incident> findByExpiresAtAfter(Instant now);

    @Query(
        "select i.principalId as principalId, count(i) as incidents " +
        "from Incident i where i.timestamp >= :since group by i.principalId order by count(i) desc"
    )
    List<TopOffenderProjection> findTopOffenders(@Param("since") Instant since);

    interface TopOffenderProjection {
        String getPrincipalId();
        Long getIncidents();
    }
}

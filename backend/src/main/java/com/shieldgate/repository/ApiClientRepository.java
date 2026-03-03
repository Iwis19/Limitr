package com.shieldgate.repository;

import com.shieldgate.domain.ApiClient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {
    Optional<ApiClient> findByApiKeyHash(String apiKeyHash);
    Optional<ApiClient> findByPrincipalId(String principalId);
}

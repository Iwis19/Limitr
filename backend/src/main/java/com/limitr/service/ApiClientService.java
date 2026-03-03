package com.limitr.service;

import com.limitr.domain.ApiClient;
import com.limitr.domain.enums.ApiClientStatus;
import com.limitr.domain.enums.ClientTier;
import com.limitr.repository.ApiClientRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final HashService hashService;

    public ApiClientService(ApiClientRepository apiClientRepository, HashService hashService) {
        this.apiClientRepository = apiClientRepository;
        this.hashService = hashService;
    }

    public Optional<ApiClient> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String apiKeyHash = hashService.sha256(apiKey);
        return apiClientRepository.findByApiKeyHash(apiKeyHash)
            .filter(client -> client.getStatus() == ApiClientStatus.ACTIVE);
    }

    @Transactional
    public ApiClient createIfAbsent(String principalId, String apiKey, ClientTier tier) {
        return apiClientRepository.findByPrincipalId(principalId).orElseGet(() -> {
            ApiClient client = new ApiClient();
            client.setPrincipalId(principalId);
            client.setApiKeyHash(hashService.sha256(apiKey));
            client.setTier(tier);
            client.setStatus(ApiClientStatus.ACTIVE);
            return apiClientRepository.save(client);
        });
    }
}

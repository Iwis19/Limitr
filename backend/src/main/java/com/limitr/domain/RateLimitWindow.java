package com.limitr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "rate_limit_windows",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rate_limit_windows_principal_epoch",
            columnNames = {"principal_id", "epoch_minute"}
        )
    },
    indexes = {
        @Index(name = "idx_rate_limit_windows_epoch", columnList = "epoch_minute")
    }
)
public class RateLimitWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principal_id", nullable = false)
    private String principalId;

    @Column(name = "epoch_minute", nullable = false)
    private Long epochMinute;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    public Long getEpochMinute() {
        return epochMinute;
    }

    public void setEpochMinute(Long epochMinute) {
        this.epochMinute = epochMinute;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

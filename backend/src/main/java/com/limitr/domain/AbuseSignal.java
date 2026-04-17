package com.limitr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "abuse_signals",
    indexes = {
        @Index(name = "idx_abuse_signals_principal_type_time", columnList = "principal_id, signal_type, timestamp"),
        @Index(name = "idx_abuse_signals_timestamp", columnList = "timestamp")
    }
)
public class AbuseSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principal_id", nullable = false)
    private String principalId;

    @Column(name = "signal_type", nullable = false)
    private String signalType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "resource_id")
    private Long resourceId;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
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

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }
}

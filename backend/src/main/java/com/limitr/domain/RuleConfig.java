package com.limitr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rule_config")
public class RuleConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Integer baseLimitPerMinute;

    @Column(nullable = false)
    private Integer throttledLimitPerMinute;

    @Column(nullable = false)
    private Integer warnThreshold;

    @Column(nullable = false)
    private Integer throttleThreshold;

    @Column(nullable = false)
    private Integer banThreshold;

    @Column(nullable = false)
    private Integer banMinutes;

    public static RuleConfig defaults() {
        RuleConfig config = new RuleConfig();
        config.setId(1L);
        config.setBaseLimitPerMinute(60);
        config.setThrottledLimitPerMinute(20);
        config.setWarnThreshold(2);
        config.setThrottleThreshold(4);
        config.setBanThreshold(7);
        config.setBanMinutes(15);
        return config;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBaseLimitPerMinute() {
        return baseLimitPerMinute;
    }

    public void setBaseLimitPerMinute(Integer baseLimitPerMinute) {
        this.baseLimitPerMinute = baseLimitPerMinute;
    }

    public Integer getThrottledLimitPerMinute() {
        return throttledLimitPerMinute;
    }

    public void setThrottledLimitPerMinute(Integer throttledLimitPerMinute) {
        this.throttledLimitPerMinute = throttledLimitPerMinute;
    }

    public Integer getWarnThreshold() {
        return warnThreshold;
    }

    public void setWarnThreshold(Integer warnThreshold) {
        this.warnThreshold = warnThreshold;
    }

    public Integer getThrottleThreshold() {
        return throttleThreshold;
    }

    public void setThrottleThreshold(Integer throttleThreshold) {
        this.throttleThreshold = throttleThreshold;
    }

    public Integer getBanThreshold() {
        return banThreshold;
    }

    public void setBanThreshold(Integer banThreshold) {
        this.banThreshold = banThreshold;
    }

    public Integer getBanMinutes() {
        return banMinutes;
    }

    public void setBanMinutes(Integer banMinutes) {
        this.banMinutes = banMinutes;
    }
}

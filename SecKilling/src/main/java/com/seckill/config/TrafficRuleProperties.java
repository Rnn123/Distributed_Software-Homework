package com.seckill.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.traffic")
public class TrafficRuleProperties {
    private double pingQps = 5;
    private int slowRequestMs = 300;
    private double slowRatioThreshold = 0.5;
    private int minRequestAmount = 5;
    private int statIntervalMs = 10000;
    private int degradeTimeWindowSeconds = 10;
}

package com.seckill.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class SentinelTrafficRuleConfig {
    public static final String PING_RESOURCE = "traffic-ping";
    public static final String SLOW_RESOURCE = "traffic-slow";

    private final TrafficRuleProperties properties;
    private String lastSignature = "";

    public SentinelTrafficRuleConfig(TrafficRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        refreshRulesIfNeeded();
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshRulesIfNeeded() {
        String signature = signature();
        if (signature.equals(lastSignature)) {
            return;
        }

        FlowRule pingRule = new FlowRule();
        pingRule.setResource(PING_RESOURCE);
        pingRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        pingRule.setCount(properties.getPingQps());
        FlowRuleManager.loadRules(List.of(pingRule));

        DegradeRule slowRule = new DegradeRule();
        slowRule.setResource(SLOW_RESOURCE);
        slowRule.setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType());
        slowRule.setCount(properties.getSlowRequestMs());
        slowRule.setSlowRatioThreshold(properties.getSlowRatioThreshold());
        slowRule.setMinRequestAmount(properties.getMinRequestAmount());
        slowRule.setStatIntervalMs(properties.getStatIntervalMs());
        slowRule.setTimeWindow(properties.getDegradeTimeWindowSeconds());
        DegradeRuleManager.loadRules(List.of(slowRule));

        lastSignature = signature;
    }

    private String signature() {
        return String.format(Locale.ROOT, "%.2f:%d:%.2f:%d:%d:%d",
                properties.getPingQps(),
                properties.getSlowRequestMs(),
                properties.getSlowRatioThreshold(),
                properties.getMinRequestAmount(),
                properties.getStatIntervalMs(),
                properties.getDegradeTimeWindowSeconds());
    }
}

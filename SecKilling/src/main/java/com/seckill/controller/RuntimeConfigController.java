package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.config.DynamicConfigProperties;
import com.seckill.config.TrafficRuleProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class RuntimeConfigController {
    private final DynamicConfigProperties dynamicConfig;
    private final TrafficRuleProperties trafficRules;
    private final String applicationName;
    private final String instanceId;

    public RuntimeConfigController(DynamicConfigProperties dynamicConfig,
                                   TrafficRuleProperties trafficRules,
                                   @Value("${spring.application.name}") String applicationName,
                                   @Value("${app.instance-id:local}") String instanceId) {
        this.dynamicConfig = dynamicConfig;
        this.trafficRules = trafficRules;
        this.applicationName = applicationName;
        this.instanceId = instanceId;
    }

    @GetMapping("/runtime")
    public Result<Map<String, Object>> runtime() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applicationName", applicationName);
        data.put("instanceId", instanceId);
        data.put("banner", dynamicConfig.getBanner());
        data.put("seckillEnabled", dynamicConfig.isSeckillEnabled());
        data.put("pingQps", trafficRules.getPingQps());
        data.put("slowRequestMs", trafficRules.getSlowRequestMs());
        data.put("degradeTimeWindowSeconds", trafficRules.getDegradeTimeWindowSeconds());
        return Result.success(data);
    }
}

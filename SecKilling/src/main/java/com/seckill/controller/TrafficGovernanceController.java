package com.seckill.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.config.DynamicConfigProperties;
import com.seckill.config.SentinelTrafficRuleConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/traffic")
public class TrafficGovernanceController {
    private final DynamicConfigProperties dynamicConfig;
    private final String instanceId;

    public TrafficGovernanceController(DynamicConfigProperties dynamicConfig,
                                       @Value("${app.instance-id:local}") String instanceId) {
        this.dynamicConfig = dynamicConfig;
        this.instanceId = instanceId;
    }

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        try (Entry ignored = SphU.entry(SentinelTrafficRuleConfig.PING_RESOURCE)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "PASS");
            data.put("instanceId", instanceId);
            data.put("banner", dynamicConfig.getBanner());
            data.put("seckillEnabled", dynamicConfig.isSeckillEnabled());
            return Result.success(data);
        } catch (BlockException ex) {
            return Result.error(blockCode(ex));
        }
    }

    @GetMapping("/slow")
    public Result<Map<String, Object>> slow(@RequestParam(defaultValue = "500") long ms) throws InterruptedException {
        try (Entry ignored = SphU.entry(SentinelTrafficRuleConfig.SLOW_RESOURCE)) {
            Thread.sleep(Math.max(0, ms));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "SLOW_OK");
            data.put("costMs", ms);
            data.put("instanceId", instanceId);
            return Result.success(data);
        } catch (BlockException ex) {
            return Result.error(blockCode(ex));
        }
    }

    @GetMapping("/degrade")
    public Result<Map<String, Object>> degrade() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "FALLBACK");
        data.put("instanceId", instanceId);
        data.put("message", CodeMsg.SERVICE_DEGRADED.getMsg());
        return Result.success(data);
    }

    private CodeMsg blockCode(BlockException ex) {
        if (ex instanceof DegradeException) {
            return CodeMsg.CIRCUIT_OPEN;
        }
        return CodeMsg.FLOW_LIMITED;
    }
}

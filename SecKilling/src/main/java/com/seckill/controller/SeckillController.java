package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.dto.SeckillSubmitResponse;
import com.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/{productId}")
    public Result<SeckillSubmitResponse> submit(@PathVariable Long productId,
                                                @RequestHeader(value = "Authorization", required = false) String authorization,
                                                @RequestHeader(value = "X-Auth-Token", required = false) String tokenHeader) {
        return seckillService.submit(productId, resolveToken(authorization, tokenHeader));
    }

    @GetMapping("/status/{orderId}")
    public Result<String> status(@PathVariable Long orderId) {
        return seckillService.queryStatus(orderId);
    }

    private String resolveToken(String authorization, String tokenHeader) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return tokenHeader;
    }
}

package com.seckill.service;

import com.seckill.dto.SeckillOrderMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SeckillOrderProcessor {
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_WAIT_STOCK = "WAIT_STOCK_CONFIRM";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SOLD_OUT = "SOLD_OUT";

    private final InventoryService inventoryService;
    private final StringRedisTemplate redisTemplate;

    public SeckillOrderProcessor(InventoryService inventoryService, StringRedisTemplate redisTemplate) {
        this.inventoryService = inventoryService;
        this.redisTemplate = redisTemplate;
    }

    public void markProcessing(Long orderId) {
        redisTemplate.opsForValue().set(statusKey(orderId), STATUS_PROCESSING, 1, TimeUnit.DAYS);
    }

    public void markWaitingStock(Long orderId) {
        redisTemplate.opsForValue().set(statusKey(orderId), STATUS_WAIT_STOCK, 1, TimeUnit.DAYS);
    }

    public void markSuccess(Long orderId) {
        redisTemplate.opsForValue().set(statusKey(orderId), STATUS_SUCCESS, 1, TimeUnit.DAYS);
    }

    public void markPaying(Long orderId) {
        redisTemplate.opsForValue().set(statusKey(orderId), STATUS_PAYING, 1, TimeUnit.DAYS);
    }

    public void markPaid(Long orderId) {
        redisTemplate.opsForValue().set(statusKey(orderId), STATUS_PAID, 1, TimeUnit.DAYS);
    }

    public void compensate(SeckillOrderMessage message, String status) {
        inventoryService.restoreRedisStock(message.getProductId());
        redisTemplate.delete(duplicateKey(message.getProductId(), message.getUserId()));
        redisTemplate.opsForValue().set(statusKey(message.getOrderId()), status, 1, TimeUnit.DAYS);
    }

    public void compensate(Long orderId, Long productId, Long userId, String status) {
        inventoryService.restoreRedisStock(productId);
        redisTemplate.delete(duplicateKey(productId, userId));
        redisTemplate.opsForValue().set(statusKey(orderId), status, 1, TimeUnit.DAYS);
    }

    public String statusKey(Long orderId) {
        return "seckill:status:" + orderId;
    }

    public String duplicateKey(Long productId, Long userId) {
        return "seckill:dup:" + productId + ":" + userId;
    }
}

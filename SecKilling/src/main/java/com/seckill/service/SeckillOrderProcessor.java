package com.seckill.service;

import com.seckill.dto.SeckillOrderMessage;
import com.seckill.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SeckillOrderProcessor {
    private static final Logger log = LoggerFactory.getLogger(SeckillOrderProcessor.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final StringRedisTemplate redisTemplate;

    public SeckillOrderProcessor(OrderService orderService, InventoryService inventoryService, StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.redisTemplate = redisTemplate;
    }

    public void process(SeckillOrderMessage message) {
        try {
            Order order = orderService.createSeckillOrder(message);
            if (order == null) {
                fail(message, "SOLD_OUT");
                return;
            }
            redisTemplate.opsForValue().set(statusKey(message.getOrderId()), "SUCCESS", 1, TimeUnit.DAYS);
        } catch (Exception ex) {
            log.error("process seckill order failed, orderId={}", message.getOrderId(), ex);
            fail(message, "FAILED");
        }
    }

    private void fail(SeckillOrderMessage message, String status) {
        inventoryService.restoreRedisStock(message.getProductId());
        redisTemplate.delete(duplicateKey(message.getProductId(), message.getUserId()));
        redisTemplate.opsForValue().set(statusKey(message.getOrderId()), status, 1, TimeUnit.DAYS);
    }

    public String statusKey(Long orderId) {
        return "seckill:status:" + orderId;
    }

    public String duplicateKey(Long productId, Long userId) {
        return "seckill:dup:" + productId + ":" + userId;
    }
}

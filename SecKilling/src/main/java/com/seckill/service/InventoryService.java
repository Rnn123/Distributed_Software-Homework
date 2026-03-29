package com.seckill.service;

import com.seckill.config.ReadOnlyDataSource;
import com.seckill.entity.Inventory;
import com.seckill.mapper.InventoryMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class InventoryService {
    private final InventoryMapper inventoryMapper;
    private final StringRedisTemplate redisTemplate;

    public InventoryService(InventoryMapper inventoryMapper, StringRedisTemplate redisTemplate) {
        this.inventoryMapper = inventoryMapper;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void warmUpStockCache() {
        List<Inventory> inventories = inventoryMapper.listAll();
        if (inventories == null) {
            return;
        }
        for (Inventory inventory : inventories) {
            redisTemplate.opsForValue().set(stockKey(inventory.getProductId()),
                    String.valueOf(inventory.getAvailableStock()), 1, TimeUnit.DAYS);
        }
    }

    @ReadOnlyDataSource
    public Inventory getByProductId(Long productId) {
        return inventoryMapper.getByProductId(productId);
    }

    public boolean reserveStockInRedis(Long productId) {
        ensureStockLoaded(productId);
        Long remain = redisTemplate.opsForValue().decrement(stockKey(productId));
        if (remain == null) {
            return false;
        }
        if (remain < 0) {
            redisTemplate.opsForValue().increment(stockKey(productId));
            return false;
        }
        return true;
    }

    public boolean deductStockInDb(Long productId) {
        return inventoryMapper.deductStock(productId) > 0;
    }

    public void restoreRedisStock(Long productId) {
        redisTemplate.opsForValue().increment(stockKey(productId));
    }

    public void ensureStockLoaded(Long productId) {
        String key = stockKey(productId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }
        Inventory inventory = inventoryMapper.getByProductId(productId);
        int stock = inventory == null ? 0 : inventory.getAvailableStock();
        redisTemplate.opsForValue().set(key, String.valueOf(stock), 1, TimeUnit.DAYS);
    }

    public String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }
}

package com.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.entity.Product;
import com.seckill.mapper.ProductMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    ProductMapper productMapper;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    public Product getProductById(long id) {
        String key = "product:" + id;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String val = ops.get(key);

        if (StringUtils.isNotEmpty(val)) {
            if ("null".equals(val)) {
                return null;
            }
            try {
                return objectMapper.readValue(val, Product.class);
            } catch (Exception e) {
                log.error("json parse error", e);
                return null;
            }
        }

        // Cache Breakdown: Mutex Lock
        String lockKey = "lock:product:" + id;
        Boolean locked = ops.setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                Product product = productMapper.getById(id);
                if (product == null) {
                    // Cache Penetration: Cache null with short TTL
                    ops.set(key, "null", 60, TimeUnit.SECONDS);
                } else {
                    // Cache Avalanche: Random TTL + Base Time
                    int ttl = 3600 + new Random().nextInt(600);
                    ops.set(key, objectMapper.writeValueAsString(product), ttl, TimeUnit.SECONDS);
                }
                return product;
            } catch (Exception e) {
                log.error("db error", e);
                return null;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // Wait and retry
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getProductById(id);
        }
    }
}

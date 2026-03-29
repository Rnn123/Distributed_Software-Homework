package com.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.config.ReadOnlyDataSource;
import com.seckill.entity.Product;
import com.seckill.mapper.ProductMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String NULL_CACHE_VALUE = "null";

    private final ProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductService(ProductMapper productMapper, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @ReadOnlyDataSource
    public List<Product> listProducts() {
        return productMapper.list();
    }

    @ReadOnlyDataSource
    public List<Product> searchProducts(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return productMapper.list();
        }
        return productMapper.search(keyword.trim());
    }

    @ReadOnlyDataSource
    public Product getProductById(long id) {
        String key = buildProductCacheKey(id);
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(cached)) {
            if (NULL_CACHE_VALUE.equals(cached)) {
                return null;
            }
            try {
                return objectMapper.readValue(cached, Product.class);
            } catch (Exception ex) {
                log.warn("parse product cache failed, productId={}", id, ex);
                redisTemplate.delete(key);
            }
        }

        String lockKey = "lock:product:" + id;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                Product product = productMapper.getById(id);
                if (product == null) {
                    redisTemplate.opsForValue().set(key, NULL_CACHE_VALUE, 60, TimeUnit.SECONDS);
                    return null;
                }
                int ttl = 3600 + new Random().nextInt(600);
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(product), ttl, TimeUnit.SECONDS);
                return product;
            } catch (Exception ex) {
                log.error("load product from db failed, productId={}", id, ex);
                return null;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return retryLoad(id, 0);
    }

    private Product retryLoad(long id, int attempt) {
        if (attempt >= 5) {
            return null;
        }
        String cached = redisTemplate.opsForValue().get(buildProductCacheKey(id));
        if (StringUtils.isBlank(cached)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return retryLoad(id, attempt + 1);
        }
        if (NULL_CACHE_VALUE.equals(cached)) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, Product.class);
        } catch (Exception ex) {
            log.warn("retry parse product cache failed, productId={}", id, ex);
            return null;
        }
    }

    private String buildProductCacheKey(long id) {
        return "product:" + id;
    }
}

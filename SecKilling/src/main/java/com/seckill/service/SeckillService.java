package com.seckill.service;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.dto.SeckillOrderMessage;
import com.seckill.dto.SeckillSubmitResponse;
import com.seckill.entity.Order;
import com.seckill.entity.Product;
import com.seckill.entity.User;
import com.seckill.util.SnowflakeIdGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillService {
    private final UserService userService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final SeckillMessagePublisher messagePublisher;
    private final SeckillOrderProcessor orderProcessor;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final StringRedisTemplate redisTemplate;

    public SeckillService(UserService userService,
                          ProductService productService,
                          InventoryService inventoryService,
                          OrderService orderService,
                          SeckillMessagePublisher messagePublisher,
                          SeckillOrderProcessor orderProcessor,
                          SnowflakeIdGenerator snowflakeIdGenerator,
                          StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.messagePublisher = messagePublisher;
        this.orderProcessor = orderProcessor;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.redisTemplate = redisTemplate;
    }

    public Result<SeckillSubmitResponse> submit(Long productId, String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        Product product = productService.getProductById(productId);
        if (product == null) {
            return Result.error(CodeMsg.PRODUCT_NOT_FOUND);
        }

        Date now = new Date();
        if (product.getStartTime() != null && now.before(product.getStartTime())) {
            return Result.error(CodeMsg.SECKILL_NOT_STARTED);
        }
        if (product.getEndTime() != null && now.after(product.getEndTime())) {
            return Result.error(CodeMsg.SECKILL_ENDED);
        }
        if (orderService.hasOrdered(user.getId(), productId)) {
            return Result.error(CodeMsg.REPEAT_SECKILL);
        }

        String duplicateKey = orderProcessor.duplicateKey(productId, user.getId());
        Boolean firstRequest = redisTemplate.opsForValue().setIfAbsent(duplicateKey, "1", 1, TimeUnit.DAYS);
        if (!Boolean.TRUE.equals(firstRequest)) {
            return Result.error(CodeMsg.REPEAT_SECKILL);
        }

        boolean reserved = inventoryService.reserveStockInRedis(productId);
        if (!reserved) {
            redisTemplate.delete(duplicateKey);
            return Result.error(CodeMsg.STOCK_NOT_ENOUGH);
        }

        long orderId = snowflakeIdGenerator.nextId();
        redisTemplate.opsForValue().set(orderProcessor.statusKey(orderId), "PROCESSING", 1, TimeUnit.DAYS);

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(user.getId());
        message.setProductId(productId);
        message.setAmount(product.getSeckillPrice());
        message.setRequestTime(System.currentTimeMillis());
        messagePublisher.publish(message);
        return Result.success(new SeckillSubmitResponse(orderId, "PROCESSING"));
    }

    public Result<String> queryStatus(Long orderId) {
        String status = redisTemplate.opsForValue().get(orderProcessor.statusKey(orderId));
        if (status == null) {
            Order order = orderService.getById(orderId);
            if (order == null) {
                return Result.error(CodeMsg.ORDER_NOT_FOUND);
            }
            status = "SUCCESS";
        }
        return Result.success(status);
    }
}

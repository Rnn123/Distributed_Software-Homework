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
        InventoryService.RedisReservationResult reserveResult = inventoryService.reserveStockInRedis(productId, duplicateKey);
        if (reserveResult == InventoryService.RedisReservationResult.DUPLICATE) {
            return Result.error(CodeMsg.REPEAT_SECKILL);
        }
        if (reserveResult == InventoryService.RedisReservationResult.SOLD_OUT) {
            return Result.error(CodeMsg.STOCK_NOT_ENOUGH);
        }

        long orderId = snowflakeIdGenerator.nextId();
        orderProcessor.markProcessing(orderId);

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(user.getId());
        message.setProductId(productId);
        message.setAmount(product.getSeckillPrice());
        message.setRequestTime(System.currentTimeMillis());
        messagePublisher.publish(message);
        return Result.success(new SeckillSubmitResponse(orderId, SeckillOrderProcessor.STATUS_PROCESSING));
    }

    public Result<String> queryStatus(Long orderId) {
        String status = redisTemplate.opsForValue().get(orderProcessor.statusKey(orderId));
        if (status != null) {
            return Result.success(status);
        }

        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error(CodeMsg.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == OrderService.STATUS_PAID) {
            return Result.success(SeckillOrderProcessor.STATUS_PAID);
        }
        if (order.getStatus() == OrderService.STATUS_UNPAID) {
            return Result.success(SeckillOrderProcessor.STATUS_SUCCESS);
        }
        if (order.getStatus() == OrderService.STATUS_CANCELED) {
            return Result.success(SeckillOrderProcessor.STATUS_SOLD_OUT);
        }
        return Result.success(SeckillOrderProcessor.STATUS_WAIT_STOCK);
    }
}

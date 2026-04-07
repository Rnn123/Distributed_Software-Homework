package com.seckill.service;

import com.seckill.config.ReadOnlyDataSource;
import com.seckill.dto.OrderCreatedMessage;
import com.seckill.dto.OrderStatusMessage;
import com.seckill.dto.StockResultMessage;
import com.seckill.entity.Inventory;
import com.seckill.entity.StockReservation;
import com.seckill.mapper.InventoryMapper;
import com.seckill.mapper.StockReservationMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class InventoryService {
    public static final int RESERVATION_RESERVED = 0;
    public static final int RESERVATION_CONFIRMED = 1;
    public static final int RESERVATION_RELEASED = 2;

    private static final DefaultRedisScript<Long> RESERVE_STOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('exists', KEYS[2]) == 1 then
                return -1
            end
            local stock = tonumber(redis.call('get', KEYS[1]))
            if stock == nil or stock <= 0 then
                return 0
            end
            redis.call('decr', KEYS[1])
            redis.call('set', KEYS[2], ARGV[1], 'EX', ARGV[2])
            return 1
            """,
            Long.class
    );

    private final InventoryMapper inventoryMapper;
    private final StockReservationMapper stockReservationMapper;
    private final TransactionMessageService transactionMessageService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.kafka.topic.stock-result}")
    private String stockResultTopic;

    public InventoryService(InventoryMapper inventoryMapper,
                            StockReservationMapper stockReservationMapper,
                            TransactionMessageService transactionMessageService,
                            StringRedisTemplate redisTemplate) {
        this.inventoryMapper = inventoryMapper;
        this.stockReservationMapper = stockReservationMapper;
        this.transactionMessageService = transactionMessageService;
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

    public RedisReservationResult reserveStockInRedis(Long productId, String duplicateKey) {
        ensureStockLoaded(productId);
        Long result = redisTemplate.execute(
                RESERVE_STOCK_SCRIPT,
                List.of(stockKey(productId), duplicateKey),
                "1",
                String.valueOf(TimeUnit.DAYS.toSeconds(1))
        );
        if (result == null) {
            return RedisReservationResult.SOLD_OUT;
        }
        if (result == 1L) {
            return RedisReservationResult.SUCCESS;
        }
        if (result == -1L) {
            return RedisReservationResult.DUPLICATE;
        }
        return RedisReservationResult.SOLD_OUT;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreated(OrderCreatedMessage message) {
        StockReservation existing = stockReservationMapper.getByOrderId(message.getOrderId());
        if (existing != null) {
            publishStockResult(message, existing.getStatus() != RESERVATION_RELEASED,
                    existing.getStatus() == RESERVATION_RELEASED ? "STOCK_NOT_ENOUGH" : null);
            return;
        }

        StockReservation reservation = new StockReservation();
        reservation.setOrderId(message.getOrderId());
        reservation.setProductId(message.getProductId());
        reservation.setUserId(message.getUserId());
        reservation.setQuantity(1);
        reservation.setStatus(RESERVATION_RESERVED);
        stockReservationMapper.insert(reservation);

        boolean frozen = inventoryMapper.freezeStock(message.getProductId()) > 0;
        if (!frozen) {
            stockReservationMapper.updateStatus(message.getOrderId(), RESERVATION_RESERVED, RESERVATION_RELEASED);
            publishStockResult(message, false, "STOCK_NOT_ENOUGH");
            return;
        }
        publishStockResult(message, true, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOrderPaid(OrderStatusMessage message) {
        StockReservation reservation = stockReservationMapper.getByOrderId(message.getOrderId());
        if (reservation == null || reservation.getStatus() == RESERVATION_CONFIRMED) {
            return;
        }
        if (reservation.getStatus() == RESERVATION_RELEASED) {
            return;
        }

        boolean confirmed = inventoryMapper.confirmFrozenStock(message.getProductId()) > 0;
        if (!confirmed) {
            throw new IllegalStateException("confirm frozen stock failed for order " + message.getOrderId());
        }
        stockReservationMapper.updateStatus(message.getOrderId(), RESERVATION_RESERVED, RESERVATION_CONFIRMED);
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

    private void publishStockResult(OrderCreatedMessage message, boolean success, String reason) {
        StockResultMessage result = new StockResultMessage();
        result.setOrderId(message.getOrderId());
        result.setUserId(message.getUserId());
        result.setProductId(message.getProductId());
        result.setSuccess(success);
        result.setReason(reason);
        transactionMessageService.saveMessage(
                "stock-result:" + message.getOrderId(),
                stockResultTopic,
                "STOCK_RESULT",
                String.valueOf(message.getOrderId()),
                result
        );
    }

    public enum RedisReservationResult {
        SUCCESS,
        DUPLICATE,
        SOLD_OUT
    }
}

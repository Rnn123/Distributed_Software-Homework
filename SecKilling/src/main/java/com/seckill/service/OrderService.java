package com.seckill.service;

import com.seckill.config.ReadOnlyDataSource;
import com.seckill.dto.SeckillOrderMessage;
import com.seckill.entity.Order;
import com.seckill.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;

    public OrderService(OrderMapper orderMapper, InventoryService inventoryService) {
        this.orderMapper = orderMapper;
        this.inventoryService = inventoryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createSeckillOrder(SeckillOrderMessage message) {
        Order existing = orderMapper.getByUserIdAndProductId(message.getUserId(), message.getProductId());
        if (existing != null) {
            return existing;
        }

        boolean deducted = inventoryService.deductStockInDb(message.getProductId());
        if (!deducted) {
            return null;
        }

        Order order = new Order();
        order.setId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setOrderNo(String.valueOf(message.getOrderId()));
        order.setAmount(message.getAmount());
        order.setStatus(0);
        order.setCreateTime(new Date(message.getRequestTime()));
        orderMapper.insert(order);
        return order;
    }

    @ReadOnlyDataSource
    public boolean hasOrdered(Long userId, Long productId) {
        return orderMapper.countByUserIdAndProductId(userId, productId) > 0;
    }

    @ReadOnlyDataSource
    public Order getById(Long orderId) {
        return orderMapper.getById(orderId);
    }

    @ReadOnlyDataSource
    public List<Order> listByUserId(Long userId) {
        return orderMapper.listByUserId(userId);
    }
}

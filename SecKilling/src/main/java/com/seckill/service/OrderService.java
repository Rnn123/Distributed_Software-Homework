package com.seckill.service;

import com.seckill.config.ReadOnlyDataSource;
import com.seckill.dto.OrderCreatedMessage;
import com.seckill.dto.OrderStatusMessage;
import com.seckill.dto.PaymentSuccessMessage;
import com.seckill.dto.SeckillOrderMessage;
import com.seckill.dto.StockResultMessage;
import com.seckill.entity.Order;
import com.seckill.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class OrderService {
    public static final int STATUS_WAIT_STOCK_CONFIRM = -1;
    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CANCELED = 2;
    public static final int STATUS_FINISHED = 3;

    private final OrderMapper orderMapper;
    private final TransactionMessageService transactionMessageService;
    private final SeckillOrderProcessor orderProcessor;

    @Value("${app.kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topic.order-paid}")
    private String orderPaidTopic;

    public OrderService(OrderMapper orderMapper,
                        TransactionMessageService transactionMessageService,
                        SeckillOrderProcessor orderProcessor) {
        this.orderMapper = orderMapper;
        this.transactionMessageService = transactionMessageService;
        this.orderProcessor = orderProcessor;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createOrderAndPublishEvent(SeckillOrderMessage message) {
        Order existing = orderMapper.getById(message.getOrderId());
        if (existing != null) {
            return existing;
        }

        Order duplicated = orderMapper.getByUserIdAndProductId(message.getUserId(), message.getProductId());
        if (duplicated != null && duplicated.getStatus() != STATUS_CANCELED) {
            return duplicated;
        }

        Order order = new Order();
        order.setId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setOrderNo(String.valueOf(message.getOrderId()));
        order.setAmount(message.getAmount());
        order.setStatus(STATUS_WAIT_STOCK_CONFIRM);
        order.setCreateTime(new Date(message.getRequestTime()));
        orderMapper.insert(order);

        OrderCreatedMessage event = new OrderCreatedMessage();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setProductId(order.getProductId());
        event.setAmount(order.getAmount());
        event.setRequestTime(message.getRequestTime());
        transactionMessageService.saveMessage(
                "order-created:" + order.getId(),
                orderCreatedTopic,
                "ORDER_CREATED",
                String.valueOf(order.getId()),
                event
        );
        orderProcessor.markWaitingStock(order.getId());
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleStockResult(StockResultMessage message) {
        Order order = orderMapper.getById(message.getOrderId());
        if (order == null) {
            return;
        }

        if (message.isSuccess()) {
            if (order.getStatus() == STATUS_UNPAID || order.getStatus() == STATUS_PAID) {
                if (order.getStatus() == STATUS_PAID) {
                    orderProcessor.markPaid(order.getId());
                } else {
                    orderProcessor.markSuccess(order.getId());
                }
                return;
            }
            if (orderMapper.markUnpaid(order.getId()) > 0) {
                orderProcessor.markSuccess(order.getId());
            }
            return;
        }

        if (order.getStatus() == STATUS_CANCELED) {
            return;
        }
        if (orderMapper.markCanceled(order.getId()) > 0) {
            orderProcessor.compensate(order.getId(), order.getProductId(), order.getUserId(), SeckillOrderProcessor.STATUS_SOLD_OUT);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(PaymentSuccessMessage message) {
        Order order = orderMapper.getById(message.getOrderId());
        if (order == null) {
            return;
        }
        if (order.getStatus() == STATUS_PAID) {
            orderProcessor.markPaid(order.getId());
            return;
        }
        if (orderMapper.markPaid(order.getId(), new Date(message.getPayTime())) <= 0) {
            return;
        }

        OrderStatusMessage event = new OrderStatusMessage();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setProductId(order.getProductId());
        event.setStatus("PAID");
        event.setEventTime(message.getPayTime());
        transactionMessageService.saveMessage(
                "order-paid:" + order.getId(),
                orderPaidTopic,
                "ORDER_PAID",
                String.valueOf(order.getId()),
                event
        );
        orderProcessor.markPaid(order.getId());
    }

    @ReadOnlyDataSource
    public boolean hasOrdered(Long userId, Long productId) {
        return orderMapper.countActiveByUserIdAndProductId(userId, productId) > 0;
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

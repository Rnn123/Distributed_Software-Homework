package com.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.PaymentSuccessMessage;
import com.seckill.dto.StockResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderDomainEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderDomainEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public OrderDomainEventConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${app.kafka.topic.stock-result}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeStockResult(String payload) {
        try {
            StockResultMessage message = objectMapper.readValue(payload, StockResultMessage.class);
            orderService.handleStockResult(message);
        } catch (Exception ex) {
            log.error("consume stock result failed", ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topic.payment-success}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumePaymentSuccess(String payload) {
        try {
            PaymentSuccessMessage message = objectMapper.readValue(payload, PaymentSuccessMessage.class);
            orderService.handlePaymentSuccess(message);
        } catch (Exception ex) {
            log.error("consume payment success failed", ex);
        }
    }
}

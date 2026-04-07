package com.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.OrderCreatedMessage;
import com.seckill.dto.OrderStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryDomainEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(InventoryDomainEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    public InventoryDomainEventConsumer(ObjectMapper objectMapper, InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "${app.kafka.topic.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderCreated(String payload) {
        try {
            OrderCreatedMessage message = objectMapper.readValue(payload, OrderCreatedMessage.class);
            inventoryService.handleOrderCreated(message);
        } catch (Exception ex) {
            log.error("consume order created failed", ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topic.order-paid}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderPaid(String payload) {
        try {
            OrderStatusMessage message = objectMapper.readValue(payload, OrderStatusMessage.class);
            inventoryService.handleOrderPaid(message);
        } catch (Exception ex) {
            log.error("consume order paid failed", ex);
        }
    }
}

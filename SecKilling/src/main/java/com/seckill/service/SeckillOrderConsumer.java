package com.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.SeckillOrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final SeckillOrderProcessor orderProcessor;

    public SeckillOrderConsumer(ObjectMapper objectMapper,
                                OrderService orderService,
                                SeckillOrderProcessor orderProcessor) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.orderProcessor = orderProcessor;
    }

    @KafkaListener(topics = "${app.kafka.topic.seckill-order-request}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        SeckillOrderMessage message = null;
        try {
            message = objectMapper.readValue(payload, SeckillOrderMessage.class);
            orderService.createOrderAndPublishEvent(message);
        } catch (Exception ex) {
            log.error("consume seckill order request failed", ex);
            if (message != null) {
                orderProcessor.compensate(message, SeckillOrderProcessor.STATUS_FAILED);
            }
        }
    }
}

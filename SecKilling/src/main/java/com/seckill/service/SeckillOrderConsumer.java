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
    private final SeckillOrderProcessor orderProcessor;

    public SeckillOrderConsumer(ObjectMapper objectMapper, SeckillOrderProcessor orderProcessor) {
        this.objectMapper = objectMapper;
        this.orderProcessor = orderProcessor;
    }

    @KafkaListener(topics = "${app.kafka.topic.seckill-order}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        try {
            SeckillOrderMessage message = objectMapper.readValue(payload, SeckillOrderMessage.class);
            orderProcessor.process(message);
        } catch (Exception ex) {
            log.error("consume seckill message failed", ex);
        }
    }
}

package com.seckill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.SeckillOrderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SeckillMessagePublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SeckillOrderConsumer seckillOrderConsumer;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${app.kafka.topic.seckill-order-request}")
    private String topic;

    public SeckillMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper,
                                   SeckillOrderConsumer seckillOrderConsumer) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.seckillOrderConsumer = seckillOrderConsumer;
    }

    public void publish(SeckillOrderMessage message) {
        String payload = toJson(message);
        if (!kafkaEnabled) {
            seckillOrderConsumer.consume(payload);
            return;
        }

        try {
            kafkaTemplate.send(topic, String.valueOf(message.getOrderId()), payload)
                    .get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            seckillOrderConsumer.consume(payload);
        }
    }

    private String toJson(SeckillOrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize seckill message failed", ex);
        }
    }
}

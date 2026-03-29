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
    private final SeckillOrderProcessor orderProcessor;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${app.kafka.topic.seckill-order}")
    private String topic;

    public SeckillMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper,
                                   SeckillOrderProcessor orderProcessor) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.orderProcessor = orderProcessor;
    }

    public void publish(SeckillOrderMessage message) {
        if (!kafkaEnabled) {
            orderProcessor.process(message);
            return;
        }

        try {
            kafkaTemplate.send(topic, String.valueOf(message.getOrderId()), toJson(message))
                    .get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            orderProcessor.process(message);
        }
    }

    private String toJson(SeckillOrderMessage message) throws JsonProcessingException {
        return objectMapper.writeValueAsString(message);
    }
}

package com.seckill.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic seckillOrderRequestTopic(@Value("${app.kafka.topic.seckill-order-request}") String topicName) {
        return TopicBuilder.name(topicName).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedTopic(@Value("${app.kafka.topic.order-created}") String topicName) {
        return TopicBuilder.name(topicName).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic stockResultTopic(@Value("${app.kafka.topic.stock-result}") String topicName) {
        return TopicBuilder.name(topicName).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic paymentSuccessTopic(@Value("${app.kafka.topic.payment-success}") String topicName) {
        return TopicBuilder.name(topicName).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic orderPaidTopic(@Value("${app.kafka.topic.order-paid}") String topicName) {
        return TopicBuilder.name(topicName).partitions(2).replicas(1).build();
    }
}

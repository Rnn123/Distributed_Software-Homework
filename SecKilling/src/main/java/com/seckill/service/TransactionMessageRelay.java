package com.seckill.service;

import com.seckill.entity.TxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class TransactionMessageRelay {
    private static final Logger log = LoggerFactory.getLogger(TransactionMessageRelay.class);

    private final TransactionMessageService transactionMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationContext applicationContext;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${app.kafka.topic.seckill-order-request}")
    private String seckillOrderRequestTopic;

    @Value("${app.kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topic.stock-result}")
    private String stockResultTopic;

    @Value("${app.kafka.topic.payment-success}")
    private String paymentSuccessTopic;

    @Value("${app.kafka.topic.order-paid}")
    private String orderPaidTopic;

    public TransactionMessageRelay(TransactionMessageService transactionMessageService,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ApplicationContext applicationContext) {
        this.transactionMessageService = transactionMessageService;
        this.kafkaTemplate = kafkaTemplate;
        this.applicationContext = applicationContext;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.dispatch-delay-ms:1000}")
    public void relayPendingMessages() {
        List<TxMessage> messages = transactionMessageService.listPending(100);
        for (TxMessage message : messages) {
            try {
                dispatch(message);
                transactionMessageService.markSent(message.getId());
            } catch (Exception ex) {
                log.error("relay tx message failed, id={}, topic={}", message.getId(), message.getTopic(), ex);
                transactionMessageService.markRetryLater(message.getId(), 3);
            }
        }
    }

    private void dispatch(TxMessage message) throws Exception {
        if (kafkaEnabled) {
            kafkaTemplate.send(message.getTopic(), message.getBizKey(), message.getPayload()).get(3, TimeUnit.SECONDS);
            return;
        }
        dispatchLocally(message.getTopic(), message.getPayload());
    }

    private void dispatchLocally(String topic, String payload) {
        if (seckillOrderRequestTopic.equals(topic)) {
            applicationContext.getBean(SeckillOrderConsumer.class).consume(payload);
            return;
        }
        if (orderCreatedTopic.equals(topic)) {
            applicationContext.getBean(InventoryDomainEventConsumer.class).consumeOrderCreated(payload);
            return;
        }
        if (stockResultTopic.equals(topic)) {
            applicationContext.getBean(OrderDomainEventConsumer.class).consumeStockResult(payload);
            return;
        }
        if (paymentSuccessTopic.equals(topic)) {
            applicationContext.getBean(OrderDomainEventConsumer.class).consumePaymentSuccess(payload);
            return;
        }
        if (orderPaidTopic.equals(topic)) {
            applicationContext.getBean(InventoryDomainEventConsumer.class).consumeOrderPaid(payload);
            return;
        }
        throw new IllegalArgumentException("unsupported topic: " + topic);
    }
}

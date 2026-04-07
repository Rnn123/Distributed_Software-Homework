package com.seckill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.entity.TxMessage;
import com.seckill.mapper.TxMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TransactionMessageService {
    private static final Logger log = LoggerFactory.getLogger(TransactionMessageService.class);

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENT = 1;

    private final TxMessageMapper txMessageMapper;
    private final ObjectMapper objectMapper;

    public TransactionMessageService(TxMessageMapper txMessageMapper, ObjectMapper objectMapper) {
        this.txMessageMapper = txMessageMapper;
        this.objectMapper = objectMapper;
    }

    public void saveMessage(String messageKey, String topic, String bizType, String bizKey, Object payload) {
        TxMessage message = new TxMessage();
        message.setMessageKey(messageKey);
        message.setTopic(topic);
        message.setBizType(bizType);
        message.setBizKey(bizKey);
        message.setPayload(toJson(payload));
        message.setStatus(STATUS_PENDING);
        message.setRetryCount(0);
        message.setNextRetryTime(new Date());
        try {
            txMessageMapper.insert(message);
        } catch (DuplicateKeyException ex) {
            log.info("tx message already exists, messageKey={}", messageKey);
        }
    }

    public List<TxMessage> listPending(int limit) {
        return txMessageMapper.listPending(new Date(), limit);
    }

    public boolean markSent(long id) {
        return txMessageMapper.markSent(id) > 0;
    }

    public void markRetryLater(long id, int delaySeconds) {
        Date nextRetryTime = new Date(System.currentTimeMillis() + delaySeconds * 1000L);
        txMessageMapper.markRetryLater(id, nextRetryTime);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize tx message failed", ex);
        }
    }
}

package com.seckill.service;

import com.seckill.dto.PaymentSuccessMessage;
import com.seckill.entity.Order;
import com.seckill.entity.PaymentRecord;
import com.seckill.mapper.PaymentRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class PaymentService {
    public static final int PAYMENT_SUCCESS = 1;

    private final PaymentRecordMapper paymentRecordMapper;
    private final TransactionMessageService transactionMessageService;

    @Value("${app.kafka.topic.payment-success}")
    private String paymentSuccessTopic;

    public PaymentService(PaymentRecordMapper paymentRecordMapper,
                          TransactionMessageService transactionMessageService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.transactionMessageService = transactionMessageService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentRecord pay(Order order) {
        PaymentRecord existing = paymentRecordMapper.getByOrderId(order.getId());
        if (existing != null) {
            return existing;
        }

        Date now = new Date();
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(order.getId());
        paymentRecord.setUserId(order.getUserId());
        paymentRecord.setAmount(order.getAmount());
        paymentRecord.setStatus(PAYMENT_SUCCESS);
        paymentRecord.setPayTime(now);
        paymentRecordMapper.insert(paymentRecord);

        PaymentSuccessMessage message = new PaymentSuccessMessage();
        message.setPaymentId(paymentRecord.getId());
        message.setOrderId(order.getId());
        message.setUserId(order.getUserId());
        message.setProductId(order.getProductId());
        message.setAmount(order.getAmount());
        message.setPayTime(now.getTime());
        transactionMessageService.saveMessage(
                "payment-success:" + order.getId(),
                paymentSuccessTopic,
                "PAYMENT_SUCCESS",
                String.valueOf(order.getId()),
                message
        );
        return paymentRecord;
    }
}

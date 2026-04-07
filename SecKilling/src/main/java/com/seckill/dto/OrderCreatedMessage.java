package com.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreatedMessage {
    private Long orderId;
    private Long userId;
    private Long productId;
    private BigDecimal amount;
    private Long requestTime;
}

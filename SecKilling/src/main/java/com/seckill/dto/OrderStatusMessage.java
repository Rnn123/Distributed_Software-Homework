package com.seckill.dto;

import lombok.Data;

@Data
public class OrderStatusMessage {
    private Long orderId;
    private Long userId;
    private Long productId;
    private String status;
    private Long eventTime;
}

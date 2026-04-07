package com.seckill.dto;

import lombok.Data;

@Data
public class StockResultMessage {
    private Long orderId;
    private Long userId;
    private Long productId;
    private boolean success;
    private String reason;
}

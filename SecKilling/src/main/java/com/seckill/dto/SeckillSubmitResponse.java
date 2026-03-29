package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeckillSubmitResponse {
    private Long orderId;
    private String status;
}

package com.seckill.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StockReservation {
    private Long id;
    private Long orderId;
    private Long productId;
    private Long userId;
    private Integer quantity;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}

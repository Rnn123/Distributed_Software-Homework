package com.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Long id;
    private Long userId;
    private Long productId;
    private String orderNo;
    private BigDecimal amount;
    private Integer status;
    private Date createTime;
    private Date payTime;
}

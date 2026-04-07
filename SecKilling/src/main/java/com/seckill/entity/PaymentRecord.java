package com.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentRecord {
    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private Integer status;
    private Date payTime;
    private Date createTime;
    private Date updateTime;
}

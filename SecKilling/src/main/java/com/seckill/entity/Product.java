package com.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Product {
    private Long id;
    private String name;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private String imageUrl;
    private String description;
    private Date startTime;
    private Date endTime;
    private Date createTime;
}

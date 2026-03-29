package com.seckill.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Inventory {
    private Long id;
    private Long productId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer frozenStock;
    private Integer version;
    private Date updateTime;
}

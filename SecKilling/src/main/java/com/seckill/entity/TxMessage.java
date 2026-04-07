package com.seckill.entity;

import lombok.Data;

import java.util.Date;

@Data
public class TxMessage {
    private Long id;
    private String messageKey;
    private String topic;
    private String bizType;
    private String bizKey;
    private String payload;
    private Integer status;
    private Integer retryCount;
    private Date nextRetryTime;
    private Date createTime;
    private Date updateTime;
}

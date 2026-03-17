package com.seckill.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Date createTime;
    private Date updateTime;
}

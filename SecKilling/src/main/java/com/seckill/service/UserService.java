package com.seckill.service;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    public Result<User> login(String phone, String password) {
        if (StringUtils.isEmpty(phone)) {
            return Result.error(CodeMsg.MOBILE_EMPTY);
        }
        if (StringUtils.isEmpty(password)) {
            return Result.error(CodeMsg.PASSWORD_EMPTY);
        }
        
        // 判断手机号是否存在
        User user = userMapper.getByPhone(phone);
        if (user == null) {
            return Result.error(CodeMsg.MOBILE_NOT_EXIST);
        }
        
        // 验证密码 (注意：实际生产环境需加盐MD5或BCrypt)
        // 这里简化，直接明文或自行实现简单比对
        // 假设数据库存的是明文或者简单hash，这里暂做明文比对演示
        if (!user.getPassword().equals(password)) {
            return Result.error(CodeMsg.PASSWORD_ERROR);
        }

        user.setPassword(""); // 不返回密码
        return Result.success(user);
    }

    public Result<User> register(String username, String phone, String password) {
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password)) {
            return Result.error(CodeMsg.BIND_ERROR);
        }

        User existUser = userMapper.getByPhone(phone);
        if (existUser != null) {
            return Result.error(new CodeMsg(500216, "该手机号已注册"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(password); // 实际应加密存储
        user.setCreateTime(new Date());

        userMapper.insert(user);
        
        user.setPassword("");
        return Result.success(user);
    }
}

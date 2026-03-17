package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.User;
import com.seckill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public Result<User> login(@RequestParam String phone, @RequestParam String password) {
        // 实际开发中参数通常使用DTO
        return userService.login(phone, password);
    }

    @PostMapping("/register")
    public Result<User> register(@RequestParam String username, @RequestParam String phone, @RequestParam String password) {
        return userService.register(username, phone, password);
    }
}

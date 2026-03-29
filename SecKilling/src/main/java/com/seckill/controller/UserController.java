package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.dto.LoginRequest;
import com.seckill.dto.LoginResponse;
import com.seckill.dto.RegisterRequest;
import com.seckill.entity.User;
import com.seckill.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/me")
    public Result<User> currentUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader(value = "X-Auth-Token", required = false) String tokenHeader) {
        User user = userService.getUserByToken(resolveToken(authorization, tokenHeader));
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        return Result.success(user);
    }

    private String resolveToken(String authorization, String tokenHeader) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return tokenHeader;
    }
}

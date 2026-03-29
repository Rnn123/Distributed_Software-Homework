package com.seckill.service;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.config.ReadOnlyDataSource;
import com.seckill.dto.LoginRequest;
import com.seckill.dto.LoginResponse;
import com.seckill.dto.RegisterRequest;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.util.TokenUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private static final long TOKEN_TTL_MINUTES = 30L;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    @ReadOnlyDataSource
    public Result<LoginResponse> login(LoginRequest request) {
        if (StringUtils.isBlank(request.getPhone())) {
            return Result.error(CodeMsg.MOBILE_EMPTY);
        }
        if (StringUtils.isBlank(request.getPassword())) {
            return Result.error(CodeMsg.PASSWORD_EMPTY);
        }

        User user = userMapper.getByPhone(request.getPhone());
        if (user == null) {
            return Result.error(CodeMsg.MOBILE_NOT_EXIST);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error(CodeMsg.PASSWORD_ERROR);
        }
        return Result.success(buildLoginResponse(user));
    }

    public Result<LoginResponse> register(RegisterRequest request) {
        if (StringUtils.isBlank(request.getUsername())) {
            return Result.error(CodeMsg.USERNAME_EMPTY);
        }
        if (StringUtils.isBlank(request.getPhone())) {
            return Result.error(CodeMsg.MOBILE_EMPTY);
        }
        if (StringUtils.isBlank(request.getPassword())) {
            return Result.error(CodeMsg.PASSWORD_EMPTY);
        }

        if (userMapper.getByPhone(request.getPhone()) != null || userMapper.getByUsername(request.getUsername()) != null) {
            return Result.error(CodeMsg.USER_ALREADY_EXISTS);
        }

        Date now = new Date();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
        return Result.success(buildLoginResponse(user));
    }

    @ReadOnlyDataSource
    public Result<User> getById(Long id) {
        User user = userMapper.getById(id);
        if (user == null) {
            return Result.error(CodeMsg.USER_NOT_FOUND);
        }
        return Result.success(toSafeUser(user));
    }

    @ReadOnlyDataSource
    public User getUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(buildTokenKey(token));
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        redisTemplate.expire(buildTokenKey(token), TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        User user = userMapper.getById(Long.parseLong(userId));
        return user == null ? null : toSafeUser(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = TokenUtil.newToken();
        redisTemplate.opsForValue().set(buildTokenKey(token), String.valueOf(user.getId()), TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return new LoginResponse(token, toSafeUser(user));
    }

    private User toSafeUser(User user) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setPhone(user.getPhone());
        safeUser.setEmail(user.getEmail());
        safeUser.setCreateTime(user.getCreateTime());
        safeUser.setUpdateTime(user.getUpdateTime());
        safeUser.setPassword(null);
        return safeUser;
    }

    private String buildTokenKey(String token) {
        return "login:token:" + token;
    }
}

package com.seckill.common;

import lombok.Getter;

@Getter
public class CodeMsg {

    public static final CodeMsg SUCCESS = new CodeMsg(0, "success");
    public static final CodeMsg SERVER_ERROR = new CodeMsg(500100, "server error");
    public static final CodeMsg BIND_ERROR = new CodeMsg(500101, "request validation failed");
    public static final CodeMsg REQUEST_ILLEGAL = new CodeMsg(500102, "illegal request");

    public static final CodeMsg SESSION_ERROR = new CodeMsg(500210, "login session expired");
    public static final CodeMsg PASSWORD_EMPTY = new CodeMsg(500211, "password cannot be empty");
    public static final CodeMsg MOBILE_EMPTY = new CodeMsg(500212, "mobile cannot be empty");
    public static final CodeMsg MOBILE_NOT_EXIST = new CodeMsg(500214, "mobile does not exist");
    public static final CodeMsg PASSWORD_ERROR = new CodeMsg(500215, "password is incorrect");
    public static final CodeMsg USER_ALREADY_EXISTS = new CodeMsg(500216, "user already exists");
    public static final CodeMsg USERNAME_EMPTY = new CodeMsg(500217, "username cannot be empty");
    public static final CodeMsg USER_NOT_FOUND = new CodeMsg(500218, "user not found");

    public static final CodeMsg PRODUCT_NOT_FOUND = new CodeMsg(500300, "product not found");
    public static final CodeMsg STOCK_NOT_FOUND = new CodeMsg(500301, "stock not found");
    public static final CodeMsg STOCK_NOT_ENOUGH = new CodeMsg(500302, "stock not enough");
    public static final CodeMsg SECKILL_NOT_STARTED = new CodeMsg(500303, "seckill not started");
    public static final CodeMsg SECKILL_ENDED = new CodeMsg(500304, "seckill ended");
    public static final CodeMsg REPEAT_SECKILL = new CodeMsg(500305, "duplicate seckill is not allowed");
    public static final CodeMsg ORDER_NOT_FOUND = new CodeMsg(500306, "order not found");
    public static final CodeMsg ORDER_STATUS_ERROR = new CodeMsg(500307, "order status is invalid");
    public static final CodeMsg ORDER_NOT_BELONG_TO_USER = new CodeMsg(500308, "order does not belong to current user");
    public static final CodeMsg ORDER_PAYING = new CodeMsg(500309, "payment is processing");
    public static final CodeMsg PAYMENT_DUPLICATE = new CodeMsg(500310, "order has already been paid");

    private final int code;
    private final String msg;

    public CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public CodeMsg fillArgs(Object... args) {
        return new CodeMsg(this.code, String.format(this.msg, args));
    }
}

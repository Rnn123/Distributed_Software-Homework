package com.seckill.common;

import lombok.Getter;

@Getter
public class CodeMsg {

    public static final CodeMsg SUCCESS = new CodeMsg(0, "success");
    public static final CodeMsg SERVER_ERROR = new CodeMsg(500100, "服务端异常");
    public static final CodeMsg BIND_ERROR = new CodeMsg(500101, "请求参数校验失败");
    public static final CodeMsg REQUEST_ILLEGAL = new CodeMsg(500102, "非法请求");

    public static final CodeMsg SESSION_ERROR = new CodeMsg(500210, "登录状态已失效");
    public static final CodeMsg PASSWORD_EMPTY = new CodeMsg(500211, "密码不能为空");
    public static final CodeMsg MOBILE_EMPTY = new CodeMsg(500212, "手机号不能为空");
    public static final CodeMsg MOBILE_NOT_EXIST = new CodeMsg(500214, "手机号不存在");
    public static final CodeMsg PASSWORD_ERROR = new CodeMsg(500215, "密码错误");
    public static final CodeMsg USER_ALREADY_EXISTS = new CodeMsg(500216, "用户已存在");
    public static final CodeMsg USERNAME_EMPTY = new CodeMsg(500217, "用户名不能为空");
    public static final CodeMsg USER_NOT_FOUND = new CodeMsg(500218, "用户不存在");

    public static final CodeMsg PRODUCT_NOT_FOUND = new CodeMsg(500300, "商品不存在");
    public static final CodeMsg STOCK_NOT_FOUND = new CodeMsg(500301, "库存不存在");
    public static final CodeMsg STOCK_NOT_ENOUGH = new CodeMsg(500302, "库存不足");
    public static final CodeMsg SECKILL_NOT_STARTED = new CodeMsg(500303, "秒杀尚未开始");
    public static final CodeMsg SECKILL_ENDED = new CodeMsg(500304, "秒杀已结束");
    public static final CodeMsg REPEAT_SECKILL = new CodeMsg(500305, "请勿重复下单");
    public static final CodeMsg ORDER_NOT_FOUND = new CodeMsg(500306, "订单不存在");

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

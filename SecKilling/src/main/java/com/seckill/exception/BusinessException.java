package com.seckill.exception;

import com.seckill.common.CodeMsg;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final CodeMsg codeMsg;

    public BusinessException(CodeMsg codeMsg) {
        super(codeMsg.getMsg());
        this.codeMsg = codeMsg;
    }
}

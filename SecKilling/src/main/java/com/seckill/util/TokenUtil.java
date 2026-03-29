package com.seckill.util;

import java.util.UUID;

public final class TokenUtil {
    private TokenUtil() {
    }

    public static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

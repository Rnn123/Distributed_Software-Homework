package com.seckill.context;

import com.seckill.config.DataSourceType;

public final class DataSourceContextHolder {
    private static final ThreadLocal<DataSourceType> HOLDER = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void set(DataSourceType dataSourceType) {
        HOLDER.set(dataSourceType);
    }

    public static DataSourceType get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

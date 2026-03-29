package com.seckill.config;

import com.seckill.context.DataSourceContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.get();
        return dataSourceType == null ? DataSourceType.WRITE : dataSourceType;
    }
}

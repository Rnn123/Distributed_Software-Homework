package com.seckill.config;

import com.seckill.context.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReadOnlyDataSourceAspect {

    @Around("@annotation(com.seckill.config.ReadOnlyDataSource)")
    public Object routeReadDataSource(ProceedingJoinPoint point) throws Throwable {
        try {
            DataSourceContextHolder.set(DataSourceType.READ);
            return point.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}

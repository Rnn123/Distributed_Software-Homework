package com.seckill.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.dynamic")
public class DynamicConfigProperties {
    private String banner = "local-config";
    private boolean seckillEnabled = true;
}

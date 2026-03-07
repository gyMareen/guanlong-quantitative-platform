package com.guanlong.signal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "collector")
public class CollectorConfig {

    private long interval = 15000;
    private int retryCount = 3;
    private int elementTimeout = 10;
}

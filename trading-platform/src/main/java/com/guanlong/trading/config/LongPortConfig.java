package com.guanlong.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "longport")
public class LongPortConfig {

    private String appKey;
    private String appSecret;
    private String accessToken;
}

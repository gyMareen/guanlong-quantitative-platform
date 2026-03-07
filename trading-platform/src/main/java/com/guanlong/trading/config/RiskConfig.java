package com.guanlong.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "risk")
public class RiskConfig {

    private BigDecimal maxSinglePosition = new BigDecimal("0.20");
    private BigDecimal maxTotalPosition = new BigDecimal("0.95");
    private BigDecimal maxDailyLoss = new BigDecimal("0.05");
    private BigDecimal maxWeeklyLoss = new BigDecimal("0.10");
    private BigDecimal maxPriceDeviation = new BigDecimal("0.04");
    private BigDecimal minTradeAmount = new BigDecimal("20");
}

package com.guanlong.trading.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskRule {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String description;
    private String paramsJson;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 风控参数常量
    public static final String RULE_POSITION_LIMIT = "position_limit";
    public static final String RULE_TRADE_LIMIT = "trade_limit";
    public static final String RULE_LOSS_LIMIT = "loss_limit";
    public static final String RULE_CLOSE_RULE = "close_rule";
}

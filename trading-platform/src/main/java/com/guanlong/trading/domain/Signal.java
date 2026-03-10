package com.guanlong.trading.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("rebalance_qs")
public class Signal {

    private Long id;
    private String symbol;
    private String action;
    private BigDecimal targetWeight;
    private Integer targetPosition;
    private BigDecimal score;
    private BigDecimal price;
    private String strategy;
    private String strategyVersion;
    private String paramsHash;
    private String source;
    private String note;
    private String batchId;

    @TableField("ts")
    private LocalDateTime timestamp;

    private LocalDateTime createdAt;

    public enum Action {
        BUY, SELL, CLOSE, TARGET
    }
}

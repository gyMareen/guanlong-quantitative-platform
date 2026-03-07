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
public class Position {

    private Long id;
    private String symbol;
    private Integer qty;
    private Integer availableQty;
    private BigDecimal costPrice;
    private BigDecimal mktPrice;
    private BigDecimal marketValue;
    private BigDecimal pnl;
    private BigDecimal pnlRatio;
    private String market;
    private String accountId;
    private LocalDateTime updatedAt;
}

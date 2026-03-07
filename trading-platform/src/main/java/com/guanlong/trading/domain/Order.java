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
public class Order {

    private Long id;
    private String batchId;
    private Long signalId;
    private String symbol;
    private String side;
    private String orderType;
    private BigDecimal price;
    private Integer qty;
    private Integer filledQty;
    private BigDecimal avgPrice;
    private String status;
    private String broker;
    private String brokerOrderId;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Side {
        BUY, SELL
    }

    public enum Status {
        PENDING, SUBMITTED, PARTIAL_FILLED, FILLED, CANCELLED, REJECTED
    }

    public enum OrderType {
        MARKET, LIMIT
    }
}

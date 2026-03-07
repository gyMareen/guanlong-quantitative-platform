package com.guanlong.signal.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceData {

    private String name;
    private String symbol;
    private BigDecimal allocationCur;
    private BigDecimal allocationTar;
    private BigDecimal refPrice;
    private String date;
    private String source;

    public boolean isValid() {
        return symbol != null && !symbol.isEmpty()
                && allocationCur != null
                && allocationTar != null;
    }
}

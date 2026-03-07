package com.guanlong.signal.parser;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RebalanceParser {

    private static final String SOURCE = "futu_rebalance";

    public List<RebalanceData> parseItems(List<WebElement> items) {
        List<RebalanceData> result = new ArrayList<>();

        for (WebElement item : items) {
            try {
                RebalanceData data = parseItem(item);
                if (data != null && data.isValid()) {
                    result.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to parse item: {}", e.getMessage());
            }
        }

        log.info("Parsed {} valid rebalance data items", result.size());
        return result;
    }

    private RebalanceData parseItem(WebElement item) {
        // 解析单个调仓记录
        // 需要根据实际的 UI 结构调整

        String name = getText(item, "stock_name");
        String symbol = extractSymbol(name);
        String curAllocStr = getText(item, "current_allocation");
        String tarAllocStr = getText(item, "target_allocation");
        String refPriceStr = getText(item, "ref_price");
        String date = getText(item, "date_text");

        if (StrUtil.isEmpty(symbol)) {
            return null;
        }

        return RebalanceData.builder()
                .name(name)
                .symbol(symbol)
                .allocationCur(parsePercent(curAllocStr))
                .allocationTar(parsePercent(tarAllocStr))
                .refPrice(parsePrice(refPriceStr))
                .date(date)
                .source(SOURCE)
                .build();
    }

    private String getText(WebElement parent, String id) {
        try {
            WebElement element = parent.findElement(
                    io.appium.java_client.AppiumBy.id(id)
            );
            return element.getText();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractSymbol(String name) {
        if (StrUtil.isEmpty(name)) {
            return null;
        }
        // 从 "AAPL 苹果公司" 提取 "AAPL.US"
        // 需要根据实际格式调整
        String[] parts = name.split("\\s+");
        if (parts.length > 0) {
            String symbol = parts[0].trim();
            // 假设美股
            if (!symbol.contains(".")) {
                symbol = symbol + ".US";
            }
            return symbol;
        }
        return null;
    }

    private BigDecimal parsePercent(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        try {
            // "5.2%" -> 0.052
            String cleaned = value.replace("%", "").trim();
            return new BigDecimal(cleaned).divide(new BigDecimal("100"));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parsePrice(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        try {
            String cleaned = value.replace("$", "").replace(",", "").trim();
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}

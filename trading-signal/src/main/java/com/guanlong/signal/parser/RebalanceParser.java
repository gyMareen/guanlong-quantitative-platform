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
        String name = getText(item, "cn.futu.trader:id/stock_name_tv");
        String symbol = getText(item, "cn.futu.trader:id/stock_code_tv");
        String changeText = getText(item, "cn.futu.trader:id/position_change_content_tv");
        String priceText = getText(item, "cn.futu.trader:id/position_price_tv");

        if (StrUtil.isEmpty(symbol)) {
            return null;
        }

        // 解析 "0.00%->19.73%" 格式
        BigDecimal[] allocations = parseAllocationChange(changeText);
        BigDecimal refPrice = extractPrice(priceText);

        return RebalanceData.builder()
                .name(name)
                .symbol(symbol + ".US")
                .allocationCur(allocations[0])
                .allocationTar(allocations[1])
                .refPrice(refPrice)
                .source(SOURCE)
                .build();
    }

    private BigDecimal[] parseAllocationChange(String text) {
        if (StrUtil.isEmpty(text)) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
        try {
            String[] parts = text.split("->");
            BigDecimal cur = parsePercent(parts[0].trim());
            BigDecimal tar = parsePercent(parts[1].trim());
            return new BigDecimal[]{cur, tar};
        } catch (Exception e) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
    }

    private BigDecimal extractPrice(String text) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            // "参考成交价 688.200" -> 688.200
            String[] parts = text.split("\\s+");
            if (parts.length > 1) {
                return new BigDecimal(parts[1]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse price: {}", text);
        }
        return null;
    }

    private String getText(WebElement parent, String resourceId) {
        try {
            WebElement element = parent.findElement(
                    io.appium.java_client.AppiumBy.id(resourceId)
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

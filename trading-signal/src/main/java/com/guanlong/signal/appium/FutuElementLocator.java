package com.guanlong.signal.appium;

import com.guanlong.signal.config.CollectorConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FutuElementLocator {

    private final AppiumDriverManager driverManager;
    private final CollectorConfig config;

    // 富途 App 调仓历史页面的元素定位
    // 注意：这些 ID 可能需要根据实际 App 版本调整
    private static final String REBALANCE_HISTORY_RV = "quote_portfolio_position_history_rv";
    private static final String STOCK_NAME = "stock_name";
    private static final String CURRENT_ALLOCATION = "current_allocation";
    private static final String TARGET_ALLOCATION = "target_allocation";
    private static final String REF_PRICE = "ref_price";
    private static final String DATE_TEXT = "date_text";

    public List<WebElement> findRebalanceHistoryItems() {
        AndroidDriver driver = driverManager.getDriver();

        try {
            // 等待调仓历史列表加载
            Thread.sleep(2000);

            // 查找调仓历史 RecyclerView
            WebElement recyclerView = driver.findElement(
                    AppiumBy.id(REBALANCE_HISTORY_RV)
            );

            // 获取所有行
            List<WebElement> items = recyclerView.findElements(
                    AppiumBy.className("android.widget.LinearLayout")
            );

            log.info("Found {} rebalance history items", items.size());
            return items;

        } catch (Exception e) {
            log.error("Failed to find rebalance history items", e);
            throw new RuntimeException("Failed to find rebalance history items", e);
        }
    }

    public String getTextSafely(WebElement parent, String elementId) {
        try {
            WebElement element = parent.findElement(AppiumBy.id(elementId));
            return element.getText();
        } catch (Exception e) {
            log.debug("Element not found: {}", elementId);
            return null;
        }
    }

    public void navigateToRebalanceHistory() {
        AndroidDriver driver = driverManager.getDriver();

        try {
            log.info("Navigating to rebalance history page...");

            // 1. 点击行情 Tab
            // 2. 点击持仓
            // 3. 点击调仓历史

            // 这里需要根据实际的 App 导航流程实现
            // 示例代码：
            // driver.findElement(AppiumBy.id("tab_quote")).click();
            // Thread.sleep(1000);
            // driver.findElement(AppiumBy.id("portfolio")).click();
            // Thread.sleep(1000);
            // driver.findElement(AppiumBy.id("position_history")).click();

            Thread.sleep(2000);
            log.info("Navigated to rebalance history page");

        } catch (Exception e) {
            log.error("Failed to navigate to rebalance history", e);
            throw new RuntimeException("Failed to navigate to rebalance history", e);
        }
    }

    public void scrollDown() {
        AndroidDriver driver = driverManager.getDriver();
        try {
            int height = driver.manage().window().getSize().getHeight();
            int width = driver.manage().window().getSize().getWidth();
            driver.swipe(
                    width / 2, (int) (height * 0.8),
                    width / 2, (int) (height * 0.2),
                    Duration.ofMillis(500)
            );
        } catch (Exception e) {
            log.warn("Failed to scroll down", e);
        }
    }

    public void goBack() {
        AndroidDriver driver = driverManager.getDriver();
        try {
            driver.navigate().back();
            Thread.sleep(500);
        } catch (Exception e) {
            log.warn("Failed to go back", e);
        }
    }
}

package com.guanlong.signal.appium;

import com.guanlong.signal.config.CollectorConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
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
            Thread.sleep(2000);

            WebElement recyclerView = driver.findElement(
                    AppiumBy.id("cn.futu.trader:id/quote_portfolio_position_history_rv")
            );

            List<WebElement> items = recyclerView.findElements(
                    AppiumBy.xpath(".//android.view.ViewGroup[@clickable='true']")
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

            // 检查是否已在调仓历史页面
            if (isOnRebalanceHistoryPage()) {
                log.info("Already on rebalance history page");
                return;
            }

            // 点击消息通知中的调仓记录进入调仓历史
            WebElement rebalanceMsg = driver.findElement(
                AppiumBy.xpath("//cn.futu.uikit.widget.textview.FtTextView[contains(@text, '组合调仓')]")
            );
            rebalanceMsg.click();
            Thread.sleep(1500);

            log.info("Navigated to rebalance history page");

        } catch (Exception e) {
            log.error("Failed to navigate to rebalance history", e);
            throw new RuntimeException("Failed to navigate to rebalance history", e);
        }
    }

    private boolean isOnRebalanceHistoryPage() {
        try {
            AndroidDriver driver = driverManager.getDriver();
            WebElement title = driver.findElement(
                AppiumBy.xpath("//cn.futu.uikit.widget.textview.FtTextView[@text='调仓历史']")
            );
            return title != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void scrollDown() {
        AndroidDriver driver = driverManager.getDriver();
        try {
            Dimension size = driver.manage().window().getSize();
            int height = size.getHeight();
            int width = size.getWidth();

            // 使用 W3C Actions API 实现滑动
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 0);

            // 从屏幕底部 80% 滑动到 20%
            Point start = new Point(width / 2, (int) (height * 0.8));
            Point end = new Point(width / 2, (int) (height * 0.2));

            // 移动到起始位置
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), start.x, start.y));
            // 按下
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            // 滑动到结束位置
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), end.x, end.y));
            // 释放
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Arrays.asList(swipe));
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

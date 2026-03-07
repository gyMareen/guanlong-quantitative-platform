package com.guanlong.signal.appium;

import com.guanlong.signal.config.AppiumConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppiumDriverManager {

    private final AppiumConfig config;
    private AndroidDriver driver;

    public AndroidDriver getDriver() {
        if (driver == null) {
            throw new IllegalStateException("Driver not initialized. Call initDriver() first.");
        }
        return driver;
    }

    public void initDriver() {
        try {
            UiAutomator2Options options = new UiAutomator2Options();
            options.setPlatformName(config.getPlatformName());
            options.setAutomationName(config.getAutomationName());
            options.setDeviceName(config.getDeviceName());
            options.setAppPackage(config.getAppPackage());
            options.setAppActivity(config.getAppActivity());
            options.setNoReset(config.isNoReset());
            options.setNewCommandTimeout(Duration.ofSeconds(config.getNewCommandTimeout()));

            URL appiumServerUrl = new URL(config.getAppiumServerUrl());
            driver = new AndroidDriver(appiumServerUrl, options);

            log.info("Appium driver initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Appium driver", e);
            throw new RuntimeException("Failed to initialize Appium driver", e);
        }
    }

    public void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
                log.info("Appium driver quit successfully");
            } catch (Exception e) {
                log.error("Failed to quit Appium driver", e);
            } finally {
                driver = null;
            }
        }
    }

    public void restartDriver() {
        log.info("Restarting Appium driver...");
        quitDriver();
        initDriver();
    }

    public boolean isDriverAlive() {
        if (driver == null) {
            return false;
        }
        try {
            driver.getCurrentPackage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

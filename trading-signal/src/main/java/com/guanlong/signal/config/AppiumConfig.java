package com.guanlong.signal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "appium")
public class AppiumConfig {

    private String host = "localhost";
    private int port = 4723;
    private String platformName = "Android";
    private String automationName = "UiAutomator2";
    private String deviceName = "emulator-5554";
    private String appPackage = "com.futuadvanced.FutuOpenD";
    private String appActivity = ".SplashActivity";
    private boolean noReset = true;
    private int newCommandTimeout = 300;

    public String getAppiumServerUrl() {
        return String.format("http://%s:%d/wd/hub", host, port);
    }
}

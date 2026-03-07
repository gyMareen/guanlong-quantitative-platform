package com.guanlong.trading.controller;

import com.guanlong.trading.config.RiskConfig;
import com.guanlong.trading.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统设置", description = "系统配置、参数管理相关接口")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final RiskConfig riskConfig;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port:8080}")
    private String serverPort;

    @Operation(summary = "获取系统配置")
    @GetMapping
    public ApiResponse<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("activeProfile", activeProfile);
        settings.put("serverPort", serverPort);
        settings.put("riskConfig", getRiskSettings());
        return ApiResponse.success(settings);
    }

    @Operation(summary = "获取风控配置")
    @GetMapping("/risk")
    public ApiResponse<Map<String, Object>> getRiskSettings() {
        Map<String, Object> riskSettings = new HashMap<>();
        riskSettings.put("maxSinglePosition", riskConfig.getMaxSinglePosition());
        riskSettings.put("maxTotalPosition", riskConfig.getMaxTotalPosition());
        riskSettings.put("maxDailyLoss", riskConfig.getMaxDailyLoss());
        riskSettings.put("maxWeeklyLoss", riskConfig.getMaxWeeklyLoss());
        riskSettings.put("maxPriceDeviation", riskConfig.getMaxPriceDeviation());
        riskSettings.put("minTradeAmount", riskConfig.getMinTradeAmount());
        return ApiResponse.success(riskSettings);
    }

    @Operation(summary = "更新风控配置")
    @PutMapping("/risk")
    public ApiResponse<Void> updateRiskSettings(@RequestBody Map<String, Object> settings) {
        // 注意：这里只是演示，实际更新需要通过配置中心或数据库
        // 在生产环境中，应该将这些配置存储在数据库中
        return ApiResponse.success();
    }

    @Operation(summary = "获取交易时间配置")
    @GetMapping("/trading-hours")
    public ApiResponse<TradingHours> getTradingHours() {
        return ApiResponse.success(new TradingHours(
                "09:30:00", "11:30:00",
                "13:00:00", "15:00:00",
                true
        ));
    }

    @Operation(summary = "获取版本信息")
    @GetMapping("/version")
    public ApiResponse<VersionInfo> getVersion() {
        return ApiResponse.success(new VersionInfo(
                "1.0.0",
                "2024-01-01",
                "Spring Boot 3.2.3",
                "Java 17"
        ));
    }

    public record TradingHours(
            String morningOpen,
            String morningClose,
            String afternoonOpen,
            String afternoonClose,
            boolean tradingEnabled
    ) {}

    public record VersionInfo(
            String version,
            String buildDate,
            String framework,
            String javaVersion
    ) {}
}

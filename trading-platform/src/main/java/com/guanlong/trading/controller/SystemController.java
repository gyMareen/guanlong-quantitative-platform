package com.guanlong.trading.controller;

import com.guanlong.trading.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统管理", description = "系统状态、健康检查相关接口")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        return ApiResponse.success(health);
    }

    @Operation(summary = "获取系统状态")
    @GetMapping("/status")
    public ApiResponse<SystemStatus> getStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return ApiResponse.success(new SystemStatus(
                System.getProperty("java.version"),
                usedMemory / 1024 / 1024,
                totalMemory / 1024 / 1024,
                runtime.availableProcessors(),
                Thread.activeCount(),
                LocalDateTime.now()
        ));
    }

    @Operation(summary = "获取系统配置信息")
    @GetMapping("/info")
    public ApiResponse<SystemInfo> getSystemInfo() {
        return ApiResponse.success(new SystemInfo(
                "Guanlong Trading Platform",
                "1.0.0",
                "Spring Boot 3.2.3",
                "Java 17",
                "PostgreSQL 14",
                "Redis 7",
                "Kafka 3.x"
        ));
    }

    public record SystemStatus(
            String javaVersion,
            long usedMemoryMB,
            long totalMemoryMB,
            int availableProcessors,
            int activeThreads,
            LocalDateTime timestamp
    ) {}

    public record SystemInfo(
            String name,
            String version,
            String framework,
            String javaVersion,
            String database,
            String cache,
            String messageQueue
    ) {}
}

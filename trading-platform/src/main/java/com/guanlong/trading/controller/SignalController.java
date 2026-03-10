package com.guanlong.trading.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guanlong.trading.domain.Signal;
import com.guanlong.trading.dto.ApiResponse;
import com.guanlong.trading.infra.persistence.SignalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "信号管理", description = "交易信号查询、统计相关接口")
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalRepository signalRepository;

    @Operation(summary = "分页查询信号")
    @GetMapping
    public ApiResponse<Page<Signal>> getSignals(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "股票代码") @RequestParam(required = false) String symbol,
            @Parameter(description = "策略") @RequestParam(required = false) String strategy,
            @Parameter(description = "来源") @RequestParam(required = false) String source,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate
    ) {
        Page<Signal> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Signal> wrapper = new LambdaQueryWrapper<>();

        if (symbol != null && !symbol.isEmpty()) {
            wrapper.eq(Signal::getSymbol, symbol);
        }
        if (strategy != null && !strategy.isEmpty()) {
            wrapper.eq(Signal::getStrategy, strategy);
        }
        if (source != null && !source.isEmpty()) {
            wrapper.eq(Signal::getSource, source);
        }
        if (startDate != null) {
            wrapper.ge(Signal::getTimestamp, startDate);
        }
        if (endDate != null) {
            wrapper.le(Signal::getTimestamp, endDate);
        }
        wrapper.orderByDesc(Signal::getTimestamp);

        return ApiResponse.success(signalRepository.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取今日信号")
    @GetMapping("/today")
    public ApiResponse<List<Signal>> getTodaySignals() {
        LambdaQueryWrapper<Signal> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Signal::getTimestamp, LocalDateTime.now().toLocalDate().atStartOfDay());
        wrapper.orderByDesc(Signal::getTimestamp);
        return ApiResponse.success(signalRepository.selectList(wrapper));
    }

    @Operation(summary = "获取信号统计")
    @GetMapping("/statistics")
    public ApiResponse<SignalStatistics> getSignalStatistics() {
        long todayCount = signalRepository.selectCount(
                new LambdaQueryWrapper<Signal>()
                        .ge(Signal::getTimestamp, LocalDateTime.now().toLocalDate().atStartOfDay())
        );

        // 模拟统计数据
        long executedCount = (long) (todayCount * 0.85);
        long pendingCount = (long) (todayCount * 0.10);
        long rejectedCount = todayCount - executedCount - pendingCount;

        return ApiResponse.success(new SignalStatistics((int) todayCount, (int) executedCount, (int) pendingCount, (int) rejectedCount));
    }

    @Operation(summary = "获取信号详情")
    @GetMapping("/{id}")
    public ApiResponse<Signal> getSignal(@PathVariable Long id) {
        Signal signal = signalRepository.selectById(id);
        if (signal == null) {
            return ApiResponse.error(404, "信号不存在");
        }
        return ApiResponse.success(signal);
    }

    public record SignalStatistics(int todayCount, int executedCount, int pendingCount, int rejectedCount) {}
}

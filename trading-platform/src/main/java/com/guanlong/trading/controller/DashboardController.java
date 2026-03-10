package com.guanlong.trading.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guanlong.trading.domain.Order;
import com.guanlong.trading.domain.Signal;
import com.guanlong.trading.dto.ApiResponse;
import com.guanlong.trading.infra.broker.BrokerAdapter;
import com.guanlong.trading.infra.persistence.OrderRepository;
import com.guanlong.trading.infra.persistence.SignalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "仪表盘", description = "仪表盘数据、概览统计相关接口")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BrokerAdapter brokerAdapter;
    private final OrderRepository orderRepository;
    private final SignalRepository signalRepository;

    @Operation(summary = "获取仪表盘概览数据")
    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> getOverview() {
        // 获取账户信息
        BigDecimal cashBalance = brokerAdapter.getAccountBalance();
        List<com.guanlong.trading.domain.Position> positions = brokerAdapter.getPositions();

        // 计算持仓市值
        BigDecimal positionValue = positions.stream()
                .map(p -> p.getMarketValue() != null ? p.getMarketValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算总资产
        BigDecimal totalEquity = cashBalance.add(positionValue);

        // 计算总盈亏
        BigDecimal totalPnL = positions.stream()
                .map(p -> p.getPnl() != null ? p.getPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 获取今日订单数
        long todayOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())
        );

        // 获取今日信号数
        int todaySignals = signalRepository.countTodaySignals();

        // 获取待处理订单数
        long pendingOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING")
        );

        return ApiResponse.success(new DashboardOverview(
                totalEquity,
                positionValue,
                cashBalance,
                totalPnL,
                positions.size(),
                (int) todayOrders,
                todaySignals,
                (int) pendingOrders
        ));
    }

    @Operation(summary = "获取账户统计")
    @GetMapping("/account-stats")
    public ApiResponse<AccountStats> getAccountStats() {
        BigDecimal cashBalance = brokerAdapter.getAccountBalance();
        List<com.guanlong.trading.domain.Position> positions = brokerAdapter.getPositions();

        BigDecimal positionValue = positions.stream()
                .map(p -> p.getMarketValue() != null ? p.getMarketValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEquity = cashBalance.add(positionValue);

        BigDecimal cashRatio = totalEquity.compareTo(BigDecimal.ZERO) > 0
                ? cashBalance.divide(totalEquity, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal positionRatio = totalEquity.compareTo(BigDecimal.ZERO) > 0
                ? positionValue.divide(totalEquity, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalPnL = positions.stream()
                .map(p -> p.getPnl() != null ? p.getPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgPnLRatio = positions.isEmpty() ? BigDecimal.ZERO :
                positions.stream()
                        .map(p -> p.getPnlRatio() != null ? p.getPnlRatio() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(positions.size()), 4, RoundingMode.HALF_UP);

        return ApiResponse.success(new AccountStats(
                totalEquity,
                cashBalance,
                positionValue,
                cashRatio,
                positionRatio,
                totalPnL,
                avgPnLRatio,
                positions.size()
        ));
    }

    @Operation(summary = "获取今日交易统计")
    @GetMapping("/today-stats")
    public ApiResponse<TodayStats> getTodayStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        // 今日订单统计
        long totalOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, todayStart)
        );
        long filledOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "FILLED")
                        .ge(Order::getUpdatedAt, todayStart)
        );
        long pendingOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING")
        );
        long rejectedOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "REJECTED")
                        .ge(Order::getUpdatedAt, todayStart)
        );

        // 今日信号统计
        int totalSignals = signalRepository.countTodaySignals();

        return ApiResponse.success(new TodayStats(
                (int) totalOrders,
                (int) filledOrders,
                (int) pendingOrders,
                (int) rejectedOrders,
                totalSignals,
                totalOrders > 0 ? (double) filledOrders / totalOrders * 100 : 0
        ));
    }

    @Operation(summary = "获取持仓汇总")
    @GetMapping("/position-summary")
    public ApiResponse<PositionSummary> getPositionSummary() {
        List<com.guanlong.trading.domain.Position> positions = brokerAdapter.getPositions();

        if (positions.isEmpty()) {
            return ApiResponse.success(new PositionSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        BigDecimal totalValue = positions.stream()
                .map(p -> p.getMarketValue() != null ? p.getMarketValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = positions.stream()
                .map(p -> p.getCostPrice() != null && p.getQty() != null
                        ? p.getCostPrice().multiply(BigDecimal.valueOf(p.getQty()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnL = totalValue.subtract(totalCost);

        BigDecimal pnlRatio = totalCost.compareTo(BigDecimal.ZERO) > 0
                ? totalPnL.divide(totalCost, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal maxPositionValue = positions.stream()
                .map(p -> p.getMarketValue() != null ? p.getMarketValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::max);

        BigDecimal maxPositionRatio = totalValue.compareTo(BigDecimal.ZERO) > 0
                ? maxPositionValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ApiResponse.success(new PositionSummary(
                positions.size(),
                totalValue,
                totalPnL,
                pnlRatio,
                maxPositionRatio
        ));
    }

    public record DashboardOverview(
            BigDecimal totalEquity,
            BigDecimal positionValue,
            BigDecimal cashBalance,
            BigDecimal totalPnL,
            int positionCount,
            int todayOrders,
            int todaySignals,
            int pendingOrders
    ) {}

    public record AccountStats(
            BigDecimal totalEquity,
            BigDecimal cashBalance,
            BigDecimal positionValue,
            BigDecimal cashRatio,
            BigDecimal positionRatio,
            BigDecimal totalPnL,
            BigDecimal avgPnLRatio,
            int positionCount
    ) {}

    public record TodayStats(
            int totalOrders,
            int filledOrders,
            int pendingOrders,
            int rejectedOrders,
            int totalSignals,
            double fillRate
    ) {}

    public record PositionSummary(
            int positionCount,
            BigDecimal totalValue,
            BigDecimal totalPnL,
            BigDecimal pnlRatio,
            BigDecimal maxPositionRatio
    ) {}
}

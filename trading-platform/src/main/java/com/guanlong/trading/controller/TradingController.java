package com.guanlong.trading.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guanlong.trading.domain.Order;
import com.guanlong.trading.domain.Signal;
import com.guanlong.trading.dto.ApiResponse;
import com.guanlong.trading.infra.persistence.OrderRepository;
import com.guanlong.trading.service.OrderExecutor;
import com.guanlong.trading.service.PositionCalculator;
import com.guanlong.trading.service.SignalMerger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "交易管理", description = "信号处理、订单管理相关接口")
@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
public class TradingController {

    private final SignalMerger signalMerger;
    private final PositionCalculator positionCalculator;
    private final OrderExecutor orderExecutor;
    private final OrderRepository orderRepository;

    @Operation(summary = "处理信号并生成订单")
    @PostMapping("/signals/process")
    public ApiResponse<List<Order>> processSignals(@RequestBody List<Signal> signals) {
        List<Signal> mergedSignals = signalMerger.mergeSignals(signals);
        List<Order> orders = positionCalculator.calculateAdjustOrders(mergedSignals);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "执行订单")
    @PostMapping("/orders/execute")
    public ApiResponse<Void> executeOrders(@RequestBody List<Order> orders) {
        orderExecutor.executeOrders(orders);
        return ApiResponse.success();
    }

    @Operation(summary = "获取活跃订单")
    @GetMapping("/orders/active")
    public ApiResponse<List<Order>> getActiveOrders() {
        return ApiResponse.success(orderRepository.findActiveOrders());
    }

    @Operation(summary = "分页查询订单")
    @GetMapping("/orders")
    public ApiResponse<Page<Order>> getOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "股票代码") @RequestParam(required = false) String symbol,
            @Parameter(description = "订单状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate
    ) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        if (symbol != null && !symbol.isEmpty()) {
            wrapper.eq(Order::getSymbol, symbol);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(Order::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(Order::getCreatedAt, endDate);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        return ApiResponse.success(orderRepository.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/orders/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(order);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id) {
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        if (!"PENDING".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            return ApiResponse.error(400, "订单状态不允许取消");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.updateById(order);

        return ApiResponse.success();
    }

    @Operation(summary = "获取订单统计")
    @GetMapping("/orders/statistics")
    public ApiResponse<OrderStatistics> getOrderStatistics() {
        long todayOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())
        );
        long pendingOrders = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING")
        );
        long todayFilled = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "FILLED")
                        .ge(Order::getUpdatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())
        );
        long todayRejected = orderRepository.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "REJECTED")
                        .ge(Order::getUpdatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())
        );

        return ApiResponse.success(new OrderStatistics((int) todayOrders, (int) pendingOrders, (int) todayFilled, (int) todayRejected));
    }

    public record OrderStatistics(int todayOrders, int pendingOrders, int todayFilled, int todayRejected) {}
}

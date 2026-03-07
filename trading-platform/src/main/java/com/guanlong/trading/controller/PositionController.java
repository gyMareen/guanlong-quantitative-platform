package com.guanlong.trading.controller;

import com.guanlong.trading.domain.Position;
import com.guanlong.trading.dto.ApiResponse;
import com.guanlong.trading.infra.broker.BrokerAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "持仓管理", description = "持仓查询、账户信息相关接口")
@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final BrokerAdapter brokerAdapter;

    @Operation(summary = "获取当前持仓")
    @GetMapping
    public ApiResponse<List<Position>> getPositions() {
        return ApiResponse.success(brokerAdapter.getPositions());
    }

    @Operation(summary = "获取账户余额")
    @GetMapping("/balance")
    public ApiResponse<BalanceInfo> getBalance() {
        return ApiResponse.success(new BalanceInfo(brokerAdapter.getAccountBalance()));
    }

    @Operation(summary = "获取账户概览")
    @GetMapping("/overview")
    public ApiResponse<AccountOverview> getAccountOverview() {
        List<Position> positions = brokerAdapter.getPositions();
        BigDecimal cashBalance = brokerAdapter.getAccountBalance();

        // 计算持仓市值
        BigDecimal positionValue = positions.stream()
                .map(Position::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算总资产
        BigDecimal totalEquity = cashBalance.add(positionValue);

        // 计算总盈亏
        BigDecimal totalPnL = positions.stream()
                .map(p -> p.getPnl() != null ? p.getPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算仓位占比
        BigDecimal positionRatio = totalEquity.compareTo(BigDecimal.ZERO) > 0
                ? positionValue.divide(totalEquity, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ApiResponse.success(new AccountOverview(
                totalEquity,
                positionValue,
                cashBalance,
                totalPnL,
                positionRatio,
                positions.size()
        ));
    }

    @Operation(summary = "获取持仓分布")
    @GetMapping("/distribution")
    public ApiResponse<List<PositionDistribution>> getPositionDistribution() {
        List<Position> positions = brokerAdapter.getPositions();
        BigDecimal totalValue = positions.stream()
                .map(Position::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ApiResponse.success(positions.stream()
                .map(p -> new PositionDistribution(
                        p.getSymbol(),
                        p.getMarketValue(),
                        totalValue.compareTo(BigDecimal.ZERO) > 0
                                ? p.getMarketValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO
                ))
                .toList());
    }

    @Operation(summary = "获取单只持仓详情")
    @GetMapping("/{symbol}")
    public ApiResponse<Position> getPosition(@PathVariable String symbol) {
        List<Position> positions = brokerAdapter.getPositions();
        return positions.stream()
                .filter(p -> symbol.equals(p.getSymbol()))
                .findFirst()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "持仓不存在"));
    }

    public record BalanceInfo(BigDecimal balance) {}

    public record AccountOverview(
            BigDecimal totalEquity,
            BigDecimal positionValue,
            BigDecimal cashBalance,
            BigDecimal totalPnL,
            BigDecimal positionRatio,
            int positionCount
    ) {}

    public record PositionDistribution(
            String symbol,
            BigDecimal marketValue,
            BigDecimal weight
    ) {}
}

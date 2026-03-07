package com.guanlong.trading.service;

import com.guanlong.trading.config.RiskConfig;
import com.guanlong.trading.domain.Order;
import com.guanlong.trading.domain.Position;
import com.guanlong.trading.domain.Signal;
import com.guanlong.trading.infra.broker.BrokerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仓位计算器
 * 负责计算目标持仓与当前持仓的差值
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PositionCalculator {

    private final BrokerAdapter brokerAdapter;
    private final RiskConfig riskConfig;

    /**
     * 计算需要调整的订单
     */
    public List<Order> calculateAdjustOrders(List<Signal> signals) {
        List<Order> orders = new ArrayList<>();

        // 获取当前持仓
        List<Position> currentPositions = brokerAdapter.getPositions();
        Map<String, Position> positionMap = currentPositions.stream()
                .collect(Collectors.toMap(Position::getSymbol, p -> p));

        // 获取账户总资产
        BigDecimal totalEquity = calculateTotalEquity(currentPositions);

        for (Signal signal : signals) {
            Order order = calculateOrder(signal, positionMap.get(signal.getSymbol()), totalEquity);
            if (order != null) {
                orders.add(order);
            }
        }

        return orders;
    }

    /**
     * 计算单个信号的订单
     */
    private Order calculateOrder(Signal signal, Position currentPosition, BigDecimal totalEquity) {
        String symbol = signal.getSymbol();

        // 计算目标市值
        BigDecimal targetValue = totalEquity.multiply(signal.getTargetWeight())
                .setScale(2, RoundingMode.HALF_UP);

        // 计算当前市值
        BigDecimal currentValue = BigDecimal.ZERO;
        int currentQty = 0;
        if (currentPosition != null) {
            currentValue = currentPosition.getMarketValue();
            currentQty = currentPosition.getQty();
        }

        // 计算差值
        BigDecimal deltaValue = targetValue.subtract(currentValue);

        // 获取当前价格
        BigDecimal currentPrice = brokerAdapter.getCurrentPrice(symbol);

        // 计算目标数量
        int targetQty = targetValue.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
        int deltaQty = targetQty - currentQty;

        // 检查是否满足最小交易金额
        BigDecimal tradeValue = currentPrice.multiply(BigDecimal.valueOf(Math.abs(deltaQty)));
        if (tradeValue.compareTo(riskConfig.getMinTradeAmount()) < 0) {
            log.debug("Trade value {} less than minimum {}, skipping {}",
                    tradeValue, riskConfig.getMinTradeAmount(), symbol);
            return null;
        }

        // 检查目标为0的情况（清仓）
        if (signal.getTargetWeight().compareTo(BigDecimal.ZERO) == 0 && currentQty > 0) {
            log.info("Target weight is 0, closing position for {}", symbol);
            return Order.builder()
                    .symbol(symbol)
                    .side(Order.Side.SELL.name())
                    .orderType(Order.OrderType.MARKET.name())
                    .qty(currentQty)
                    .build();
        }

        // 构建订单
        if (deltaQty > 0) {
            return Order.builder()
                    .symbol(symbol)
                    .side(Order.Side.BUY.name())
                    .orderType(Order.OrderType.LIMIT.name())
                    .qty(deltaQty)
                    .price(calculateLimitPrice(currentPrice, true))
                    .build();
        } else if (deltaQty < 0) {
            return Order.builder()
                    .symbol(symbol)
                    .side(Order.Side.SELL.name())
                    .orderType(Order.OrderType.LIMIT.name())
                    .qty(Math.abs(deltaQty))
                    .price(calculateLimitPrice(currentPrice, false))
                    .build();
        }

        return null;
    }

    /**
     * 计算限价（考虑价格偏离）
     */
    private BigDecimal calculateLimitPrice(BigDecimal currentPrice, boolean isBuy) {
        BigDecimal deviation = riskConfig.getMaxPriceDeviation();
        if (isBuy) {
            return currentPrice.multiply(BigDecimal.ONE.add(deviation))
                    .setScale(4, RoundingMode.HALF_UP);
        } else {
            return currentPrice.multiply(BigDecimal.ONE.subtract(deviation))
                    .setScale(4, RoundingMode.HALF_UP);
        }
    }

    /**
     * 计算总资产
     */
    private BigDecimal calculateTotalEquity(List<Position> positions) {
        BigDecimal positionValue = positions.stream()
                .map(Position::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashBalance = brokerAdapter.getAccountBalance();
        return positionValue.add(cashBalance);
    }
}

package com.guanlong.trading.infra.broker;

import com.guanlong.trading.config.LongPortConfig;
import com.guanlong.trading.domain.Order;
import com.guanlong.trading.domain.Position;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * LongPort 券商适配器实现
 */
@Slf4j
@Component
public class LongPortAdapter implements BrokerAdapter {

    private final LongPortConfig config;

    public LongPortAdapter(LongPortConfig config) {
        this.config = config;
        // TODO: 初始化 LongPort SDK
        // Config config = Config.builder()
        //     .appKey(config.getAppKey())
        //     .appSecret(config.getAppSecret())
        //     .accessToken(config.getAccessToken())
        //     .build();
        // this.httpClient = new HttpClient(config);
    }

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            // TODO: 调用 LongPort API 获取行情
            // Quote quote = httpClient.getQuote(symbol);
            // return new BigDecimal(quote.getLastDone());
            log.debug("Getting price for symbol: {}", symbol);
            return BigDecimal.ZERO; // 临时返回
        } catch (Exception e) {
            log.error("Failed to get price for symbol: {}", symbol, e);
            return null;
        }
    }

    @Override
    public List<Position> getPositions() {
        try {
            // TODO: 调用 LongPort API 获取持仓
            // List<StockPosition> positions = httpClient.getStockPositions();
            // return positions.stream().map(p -> Position.builder()
            //     .symbol(p.getSymbol())
            //     .qty(p.getQuantity())
            //     .costPrice(new BigDecimal(p.getCostPrice()))
            //     .build())
            // .collect(Collectors.toList());
            log.debug("Getting positions");
            return Collections.emptyList(); // 临时返回
        } catch (Exception e) {
            log.error("Failed to get positions", e);
            return Collections.emptyList();
        }
    }

    @Override
    public BigDecimal getAccountBalance() {
        try {
            // TODO: 调用 LongPort API 获取账户余额
            // Account account = httpClient.getAccountBalance();
            // return new BigDecimal(account.getTotalCash());
            log.debug("Getting account balance");
            return BigDecimal.ZERO; // 临时返回
        } catch (Exception e) {
            log.error("Failed to get account balance", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String submitOrder(Order order) {
        try {
            // TODO: 调用 LongPort API 下单
            // SubmitOrderResponse response = httpClient.submitOrder(
            //     order.getSymbol(),
            //     order.getSide().name(),
            //     order.getOrderType().name(),
            //     order.getPrice(),
            //     order.getQty()
            // );
            // return response.getOrderId();
            log.info("Submitting order: {} {} {} @ {}", order.getSide(), order.getQty(), order.getSymbol(), order.getPrice());
            return "ORDER_" + System.currentTimeMillis(); // 临时返回
        } catch (Exception e) {
            log.error("Failed to submit order", e);
            throw new RuntimeException("Failed to submit order", e);
        }
    }

    @Override
    public Order.Status getOrderStatus(String orderId) {
        try {
            // TODO: 调用 LongPort API 查询订单状态
            // OrderDetail detail = httpClient.getOrderDetail(orderId);
            // return mapStatus(detail.getStatus());
            log.debug("Getting order status for: {}", orderId);
            return Order.Status.FILLED; // 临时返回
        } catch (Exception e) {
            log.error("Failed to get order status", e);
            return Order.Status.REJECTED;
        }
    }

    @Override
    public boolean cancelOrder(String orderId) {
        try {
            // TODO: 调用 LongPort API 取消订单
            // return httpClient.cancelOrder(orderId);
            log.info("Cancelling order: {}", orderId);
            return true; // 临时返回
        } catch (Exception e) {
            log.error("Failed to cancel order", e);
            return false;
        }
    }
}

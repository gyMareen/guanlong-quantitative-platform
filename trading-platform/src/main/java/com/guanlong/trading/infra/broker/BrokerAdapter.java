package com.guanlong.trading.infra.broker;

import com.guanlong.trading.domain.Order;
import com.guanlong.trading.domain.Position;

import java.math.BigDecimal;
import java.util.List;

/**
 * 券商适配器接口
 */
public interface BrokerAdapter {

    /**
     * 获取股票当前价格
     */
    BigDecimal getCurrentPrice(String symbol);

    /**
     * 获取账户持仓
     */
    List<Position> getPositions();

    /**
     * 获取账户现金余额
     */
    BigDecimal getAccountBalance();

    /**
     * 提交订单
     */
    String submitOrder(Order order);

    /**
     * 查询订单状态
     */
    Order.Status getOrderStatus(String orderId);

    /**
     * 取消订单
     */
    boolean cancelOrder(String orderId);
}

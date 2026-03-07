package com.guanlong.trading.service;

import com.guanlong.trading.domain.Order;
import com.guanlong.trading.infra.broker.BrokerAdapter;
import com.guanlong.trading.infra.persistence.OrderRepository;
import com.guanlong.trading.kafka.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单执行器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutor {

    private final BrokerAdapter brokerAdapter;
    private final RiskService riskService;
    private final EventProducer eventProducer;
    private final OrderRepository orderRepository;

    /**
     * 执行订单列表
     */
    public void executeOrders(List<Order> orders) {
        for (Order order : orders) {
            try {
                executeOrder(order);
            } catch (Exception e) {
                log.error("Failed to execute order for {}", order.getSymbol(), e);
                order.setStatus(Order.Status.REJECTED.name());
                order.setErrorMsg(e.getMessage());
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.updateById(order);
            }
        }
    }

    /**
     * 执行单个订单
     */
    public void executeOrder(Order order) {
        log.info("Executing order: {} {} {} shares of {}",
                order.getSide(), order.getOrderType(), order.getQty(), order.getSymbol());

        // 获取市场价格进行风控检查
        BigDecimal marketPrice = brokerAdapter.getCurrentPrice(order.getSymbol());

        // 风控检查
        if (!riskService.checkCircuitBreaker()) {
            log.warn("Order rejected: circuit breaker active");
            order.setStatus(Order.Status.REJECTED.name());
            order.setErrorMsg("Circuit breaker active");
            orderRepository.insert(order);
            eventProducer.sendRiskRejectEvent(order.getSymbol(), "Circuit breaker active");
            return;
        }

        if (!riskService.checkPriceDeviation(order.getPrice(), marketPrice)) {
            log.warn("Order rejected by risk control: price deviation for {}", order.getSymbol());
            order.setStatus(Order.Status.REJECTED.name());
            order.setErrorMsg("Price deviation exceeded");
            orderRepository.insert(order);
            eventProducer.sendRiskRejectEvent(order.getSymbol(), "Price deviation exceeded");
            return;
        }

        // 设置初始状态
        order.setStatus(Order.Status.PENDING.name());
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.insert(order);

        try {
            // 提交订单
            String brokerOrderId = brokerAdapter.submitOrder(order);
            order.setBrokerOrderId(brokerOrderId);
            order.setStatus(Order.Status.SUBMITTED.name());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.updateById(order);

            // 发送事件
            eventProducer.sendOrderCreatedEvent(order.getSymbol(), order.getId());

            log.info("Order submitted successfully: {} -> {}", order.getId(), brokerOrderId);

        } catch (Exception e) {
            log.error("Failed to submit order", e);
            order.setStatus(Order.Status.REJECTED.name());
            order.setErrorMsg(e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.updateById(order);
            throw e;
        }
    }

    /**
     * 更新订单状态
     */
    public void updateOrderStatus(Order order, Order.Status newStatus) {
        order.setStatus(newStatus.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.updateById(order);

        if (newStatus == Order.Status.FILLED) {
            eventProducer.sendOrderFilledEvent(order.getSymbol(), order.getId(), order.getFilledQty());
        }
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(Order order) {
        if (!Order.Status.PENDING.equals(Order.Status.valueOf(order.getStatus())) &&
            !Order.Status.SUBMITTED.equals(Order.Status.valueOf(order.getStatus()))) {
            log.warn("Cannot cancel order with status: {}", order.getStatus());
            return false;
        }

        try {
            boolean cancelled = brokerAdapter.cancelOrder(order.getBrokerOrderId());
            if (cancelled) {
                order.setStatus(Order.Status.CANCELLED.name());
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.updateById(order);
                log.info("Order cancelled: {}", order.getId());
            }
            return cancelled;
        } catch (Exception e) {
            log.error("Failed to cancel order: {}", order.getId(), e);
            return false;
        }
    }
}

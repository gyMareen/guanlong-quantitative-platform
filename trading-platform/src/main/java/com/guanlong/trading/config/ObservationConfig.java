package com.guanlong.trading.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {

    // 订单计数器
    @Bean
    public Counter orderCounter(MeterRegistry registry) {
        return Counter.builder("trading.orders.total")
                .description("Total number of orders")
                .tag("type", "all")
                .register(registry);
    }

    // 成功订单计数器
    @Bean
    public Counter successOrderCounter(MeterRegistry registry) {
        return Counter.builder("trading.orders.success")
                .description("Number of successful orders")
                .register(registry);
    }

    // 失败订单计数器
    @Bean
    public Counter failedOrderCounter(MeterRegistry registry) {
        return Counter.builder("trading.orders.failed")
                .description("Number of failed orders")
                .register(registry);
    }

    // 风控拒绝计数器
    @Bean
    public Counter riskRejectCounter(MeterRegistry registry) {
        return Counter.builder("trading.risk.reject")
                .description("Number of orders rejected by risk control")
                .register(registry);
    }

    // 交易金额计数器
    @Bean
    public Counter tradeAmountCounter(MeterRegistry registry) {
        return Counter.builder("trading.amount.total")
                .description("Total trade amount in USD")
                .register(registry);
    }

    // 信号处理计时器
    @Bean
    public Timer signalProcessingTimer(MeterRegistry registry) {
        return Timer.builder("trading.signal.processing.time")
                .description("Time taken to process signals")
                .register(registry);
    }

    // Kafka 消费延迟
    private final AtomicLong kafkaLag = new AtomicLong(0);

    @Bean
    public Gauge kafkaLagGauge(MeterRegistry registry) {
        return Gauge.builder("kafka.consumer.lag", kafkaLag, AtomicLong::get)
                .description("Kafka consumer lag")
                .register(registry);
    }

    public void updateKafkaLag(long lag) {
        kafkaLag.set(lag);
    }

    // 日盈亏
    private final AtomicLong dailyPnL = new AtomicLong(0);

    @Bean
    public Gauge dailyPnLGauge(MeterRegistry registry) {
        return Gauge.builder("trading.pnl.daily", dailyPnL, AtomicLong::get)
                .description("Daily profit and loss")
                .register(registry);
    }

    public void updateDailyPnL(long pnl) {
        dailyPnL.set(pnl);
    }

    // 持仓数量
    private final AtomicLong positionCount = new AtomicLong(0);

    @Bean
    public Gauge positionCountGauge(MeterRegistry registry) {
        return Gauge.builder("trading.positions.count", positionCount, AtomicLong::get)
                .description("Number of active positions")
                .register(registry);
    }

    public void updatePositionCount(int count) {
        positionCount.set(count);
    }

    // 熔断状态
    private final AtomicLong circuitBreakerActive = new AtomicLong(0);

    @Bean
    public Gauge circuitBreakerGauge(MeterRegistry registry) {
        return Gauge.builder("trading.circuit_breaker.active", circuitBreakerActive, AtomicLong::get)
                .description("Circuit breaker status (1=active, 0=inactive)")
                .register(registry);
    }

    public void setCircuitBreakerActive(boolean active) {
        circuitBreakerActive.set(active ? 1 : 0);
    }
}

package com.guanlong.trading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guanlong.trading.config.RiskConfig;
import com.guanlong.trading.domain.Order;
import com.guanlong.trading.domain.RiskRule;
import com.guanlong.trading.infra.persistence.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RiskConfig riskConfig;
    private final RiskRuleRepository riskRuleRepository;

    private static final String CIRCUIT_BREAKER_KEY = "risk:circuit_breaker:active";
    private static final String CIRCUIT_BREAKER_REASON_KEY = "risk:circuit_breaker:reason";
    private static final String CIRCUIT_BREAKER_TIME_KEY = "risk:circuit_breaker:triggered_at";

    public boolean checkPositionLimit(String symbol, BigDecimal targetWeight, BigDecimal totalEquity) {
        BigDecimal maxSinglePosition = riskConfig.getMaxSinglePosition();
        BigDecimal positionValue = totalEquity.multiply(targetWeight);
        BigDecimal maxAllowed = totalEquity.multiply(maxSinglePosition);

        if (positionValue.compareTo(maxAllowed) > 0) {
            log.warn("Position limit exceeded for {}: {} > {}", symbol, positionValue, maxAllowed);
            return false;
        }
        return true;
    }

    public boolean checkTotalPositionLimit(BigDecimal totalPositionRatio) {
        BigDecimal maxTotalPosition = riskConfig.getMaxTotalPosition();
        if (totalPositionRatio.compareTo(maxTotalPosition) > 0) {
            log.warn("Total position limit exceeded: {} > {}", totalPositionRatio, maxTotalPosition);
            return false;
        }
        return true;
    }

    public boolean checkPriceDeviation(BigDecimal orderPrice, BigDecimal marketPrice) {
        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        BigDecimal deviation = orderPrice.subtract(marketPrice).abs()
                .divide(marketPrice, 4, RoundingMode.HALF_UP);

        if (deviation.compareTo(riskConfig.getMaxPriceDeviation()) > 0) {
            log.warn("Price deviation exceeded: {} > {}", deviation, riskConfig.getMaxPriceDeviation());
            return false;
        }
        return true;
    }

    public boolean checkTradeAmount(BigDecimal tradeAmount) {
        if (tradeAmount.compareTo(riskConfig.getMinTradeAmount()) < 0) {
            log.info("Trade amount below minimum: {} < {}", tradeAmount, riskConfig.getMinTradeAmount());
            return false;
        }
        return true;
    }

    public boolean checkDailyLoss(BigDecimal currentLoss, BigDecimal totalEquity) {
        BigDecimal lossRatio = currentLoss.abs().divide(totalEquity, 4, RoundingMode.HALF_UP);
        if (lossRatio.compareTo(riskConfig.getMaxDailyLoss()) > 0) {
            log.warn("Daily loss limit exceeded: {} > {}", lossRatio, riskConfig.getMaxDailyLoss());
            return false;
        }
        return true;
    }

    public boolean checkCircuitBreaker() {
        Boolean circuitBreaker = (Boolean) redisTemplate.opsForValue().get(CIRCUIT_BREAKER_KEY);
        if (Boolean.TRUE.equals(circuitBreaker)) {
            log.warn("Circuit breaker is active, trading suspended");
            return false;
        }
        return true;
    }

    public boolean isCircuitBreakerActive() {
        Boolean circuitBreaker = (Boolean) redisTemplate.opsForValue().get(CIRCUIT_BREAKER_KEY);
        return Boolean.TRUE.equals(circuitBreaker);
    }

    public String getCircuitBreakerReason() {
        Object reason = redisTemplate.opsForValue().get(CIRCUIT_BREAKER_REASON_KEY);
        return reason != null ? reason.toString() : null;
    }

    public Long getCircuitBreakerTriggeredAt() {
        Object time = redisTemplate.opsForValue().get(CIRCUIT_BREAKER_TIME_KEY);
        return time != null ? Long.parseLong(time.toString()) : null;
    }

    public void triggerCircuitBreaker(String reason) {
        log.error("Circuit breaker triggered: {}", reason);
        redisTemplate.opsForValue().set(CIRCUIT_BREAKER_KEY, true, 1, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(CIRCUIT_BREAKER_REASON_KEY, reason);
        redisTemplate.opsForValue().set(CIRCUIT_BREAKER_TIME_KEY, System.currentTimeMillis());
    }

    public void resetCircuitBreaker() {
        log.info("Circuit breaker reset");
        redisTemplate.delete(CIRCUIT_BREAKER_KEY);
        redisTemplate.delete(CIRCUIT_BREAKER_REASON_KEY);
        redisTemplate.delete(CIRCUIT_BREAKER_TIME_KEY);
    }

    public boolean validateOrder(Order order, BigDecimal marketPrice, BigDecimal totalEquity) {
        if (!checkCircuitBreaker()) {
            return false;
        }

        if (!checkPriceDeviation(order.getPrice(), marketPrice)) {
            return false;
        }

        BigDecimal tradeAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getQty()));
        if (!checkTradeAmount(tradeAmount)) {
            return false;
        }

        return true;
    }

    public List<RiskRule> getEnabledRules() {
        return riskRuleRepository.findAllEnabled();
    }

    public RiskRule getRuleByCode(String ruleCode) {
        return riskRuleRepository.findEnabledByRuleCode(ruleCode);
    }

    public void enableRule(Long ruleId, boolean enabled) {
        riskRuleRepository.updateEnabled(ruleId, enabled);
    }

    public void updateRuleParams(Long ruleId, String paramsJson) {
        riskRuleRepository.updateParams(ruleId, paramsJson);
    }
}

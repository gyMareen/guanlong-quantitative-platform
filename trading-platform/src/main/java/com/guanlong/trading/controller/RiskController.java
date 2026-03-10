package com.guanlong.trading.controller;

import com.guanlong.trading.config.RiskConfig;
import com.guanlong.trading.domain.RiskRule;
import com.guanlong.trading.dto.ApiResponse;
import com.guanlong.trading.infra.persistence.RiskRuleRepository;
import com.guanlong.trading.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "风控管理", description = "风控规则配置、熔断机制相关接口")
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;
    private final RiskRuleRepository riskRuleRepository;
    private final RiskConfig riskConfig;

    @Operation(summary = "获取所有风控规则")
    @GetMapping("/rules")
    public ApiResponse<List<RiskRule>> getRiskRules() {
        return ApiResponse.success(riskRuleRepository.selectList(null));
    }

    @Operation(summary = "获取启用的风控规则")
    @GetMapping("/rules/enabled")
    public ApiResponse<List<RiskRule>> getEnabledRules() {
        return ApiResponse.success(riskService.getEnabledRules());
    }

    @Operation(summary = "获取风控规则详情")
    @GetMapping("/rules/{id}")
    public ApiResponse<RiskRule> getRiskRule(@PathVariable Long id) {
        RiskRule rule = riskRuleRepository.selectById(id);
        if (rule == null) {
            return ApiResponse.error(404, "规则不存在");
        }
        return ApiResponse.success(rule);
    }

    @Operation(summary = "获取当前风控配置")
    @GetMapping("/config")
    public ApiResponse<RiskConfig> getRiskConfig() {
        return ApiResponse.success(riskConfig);
    }

    @Operation(summary = "更新风控规则")
    @PutMapping("/rules/{id}")
    public ApiResponse<RiskRule> updateRiskRule(
            @PathVariable Long id,
            @RequestBody RiskRule rule) {
        RiskRule existing = riskRuleRepository.selectById(id);
        if (existing == null) {
            return ApiResponse.error(404, "规则不存在");
        }
        rule.setId(id);
        rule.setUpdatedAt(LocalDateTime.now());
        riskRuleRepository.updateById(rule);
        return ApiResponse.success(rule);
    }

    @Operation(summary = "启用/禁用风控规则")
    @PutMapping("/rules/{id}/toggle")
    public ApiResponse<Void> toggleRule(
            @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        RiskRule existing = riskRuleRepository.selectById(id);
        if (existing == null) {
            return ApiResponse.error(404, "规则不存在");
        }
        riskService.enableRule(id, enabled);
        return ApiResponse.success();
    }

    @Operation(summary = "更新规则参数")
    @PutMapping("/rules/{id}/params")
    public ApiResponse<Void> updateRuleParams(
            @PathVariable Long id,
            @Parameter(description = "参数JSON") @RequestBody String paramsJson) {
        RiskRule existing = riskRuleRepository.selectById(id);
        if (existing == null) {
            return ApiResponse.error(404, "规则不存在");
        }
        riskService.updateRuleParams(id, paramsJson);
        return ApiResponse.success();
    }

    @Operation(summary = "触发熔断")
    @PostMapping("/circuit-breaker/trigger")
    public ApiResponse<Void> triggerCircuitBreaker(
            @Parameter(description = "触发原因") @RequestParam String reason) {
        riskService.triggerCircuitBreaker(reason);
        return ApiResponse.success();
    }

    @Operation(summary = "重置熔断")
    @PostMapping("/circuit-breaker/reset")
    public ApiResponse<Void> resetCircuitBreaker() {
        riskService.resetCircuitBreaker();
        return ApiResponse.success();
    }

    @Operation(summary = "获取熔断状态")
    @GetMapping("/circuit-breaker/status")
    public ApiResponse<CircuitBreakerStatus> getCircuitBreakerStatus() {
        return ApiResponse.success(new CircuitBreakerStatus(
                riskService.isCircuitBreakerActive(),
                riskService.getCircuitBreakerReason(),
                riskService.getCircuitBreakerTriggeredAt()
        ));
    }

    @Operation(summary = "检查订单风险")
    @PostMapping("/check")
    public ApiResponse<RiskCheckResult> checkOrderRisk(@RequestBody OrderRiskCheck request) {
        boolean passed = true;
        StringBuilder message = new StringBuilder();

        // 检查熔断
        if (!riskService.checkCircuitBreaker()) {
            passed = false;
            message.append("熔断已触发; ");
        }

        // 检查价格偏离
        if (request.getOrderPrice() != null && request.getMarketPrice() != null) {
            if (!riskService.checkPriceDeviation(request.getOrderPrice(), request.getMarketPrice())) {
                passed = false;
                message.append("价格偏离超限; ");
            }
        }

        // 检查交易金额
        if (request.getOrderPrice() != null && request.getOrderQty() != null) {
            BigDecimal tradeAmount = request.getOrderPrice().multiply(BigDecimal.valueOf(request.getOrderQty()));
            if (!riskService.checkTradeAmount(tradeAmount)) {
                passed = false;
                message.append("交易金额过小; ");
            }
        }

        // 检查仓位限制
        if (request.getTargetWeight() != null && request.getTotalEquity() != null) {
            if (!riskService.checkPositionLimit(request.getSymbol(), request.getTargetWeight(), request.getTotalEquity())) {
                passed = false;
                message.append("仓位超限; ");
            }
        }

        return ApiResponse.success(new RiskCheckResult(passed, message.toString()));
    }

    public record CircuitBreakerStatus(
            boolean active,
            String reason,
            Long triggeredAt
    ) {}

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OrderRiskCheck {
        private String symbol;
        private java.math.BigDecimal orderPrice;
        private Integer orderQty;
        private java.math.BigDecimal marketPrice;
        private java.math.BigDecimal targetWeight;
        private java.math.BigDecimal totalEquity;
    }

    public record RiskCheckResult(
            boolean passed,
            String message
    ) {}
}

# 风险约束规则 (Risk Rules)

## 1. 概述

本文档定义了官龙量化一体化交易平台的风险约束规则。所有交易指令在执行前必须通过风控校验。

## 2. 风控规则

### 2.1 仓位限制

| 规则 | 参数 | 默认值 | 说明 |
|------|------|--------|------|
| 单票仓位上限 | `max_single_position` | 20% | 单只股票不超过总资产的 20% |
| 组合仓位上限 | `max_total_position` | 95% | 所有持仓不超过总资产的 95% |
| 最小现金保留 | `min_cash_reserve` | 5% | 至少保留 5% 现金 |

### 2.2 交易限制

| 规则 | 参数 | 默认值 | 说明 |
|------|------|--------|------|
| 最小交易金额 | `min_trade_amount` | $20 | 单笔交易金额不小于 20 美元 |
| 价格偏离保护 | `max_price_deviation` | 4% | 下单价格偏离市价不超过 4% |
| 单日交易次数 | `max_daily_trades` | 100 | 单日交易次数上限 |

### 2.3 亏损限制

| 规则 | 参数 | 默认值 | 说明 |
|------|------|--------|------|
| 单日亏损上限 | `max_daily_loss` | 5% | 单日亏损超过 5% 停止交易 |
| 单周亏损上限 | `max_weekly_loss` | 10% | 单周亏损超过 10% 停止交易 |
| 连续亏损天数 | `max_consecutive_loss_days` | 3 | 连续亏损超过 3 天触发熔断 |

### 2.4 清仓规则

| 规则 | 参数 | 默认值 | 说明 |
|------|------|--------|------|
| 目标为 0 清仓 | `target_zero_action` | MARKET | 目标仓位为 0 时执行清仓 |
| 清仓价格容忍 | `close_price_tolerance` | 5% | 清仓时价格容忍度更高 |

## 3. 风控流程

```
┌─────────────┐
│   交易信号   │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│  仓位检查   │────▶│   拒绝      │ 单票/组合超限
└──────┬──────┘     └─────────────┘
       │ 通过
       ▼
┌─────────────┐     ┌─────────────┐
│  金额检查   │────▶│   跳过      │ 金额 < $20
└──────┬──────┘     └─────────────┘
       │ 通过
       ▼
┌─────────────┐     ┌─────────────┐
│  价格检查   │────▶│   拒绝      │ 偏离 > 4%
└──────┬──────┘     └─────────────┘
       │ 通过
       ▼
┌─────────────┐     ┌─────────────┐
│  亏损检查   │────▶│   熔断      │ 达到亏损上限
└──────┬──────┘     └─────────────┘
       │ 通过
       ▼
┌─────────────┐
│   执行下单   │
└─────────────┘
```

## 4. 风控参数配置

### 4.1 数据库配置表

```sql
CREATE TABLE risk_rules (
    id SERIAL PRIMARY KEY,
    rule_code VARCHAR(50) UNIQUE NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    params_json JSONB NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 初始配置
INSERT INTO risk_rules (rule_code, rule_name, params_json) VALUES
('position_limit', '仓位限制', '{"max_single_position": 0.20, "max_total_position": 0.95, "min_cash_reserve": 0.05}'),
('trade_limit', '交易限制', '{"min_trade_amount": 20, "max_price_deviation": 0.04, "max_daily_trades": 100}'),
('loss_limit', '亏损限制', '{"max_daily_loss": 0.05, "max_weekly_loss": 0.10, "max_consecutive_loss_days": 3}'),
('close_rule', '清仓规则', '{"target_zero_action": "MARKET", "close_price_tolerance": 0.05}');
```

### 4.2 Redis 缓存

风控参数应缓存在 Redis 中，TTL 为 5 分钟：

```
Key: risk:rules:{rule_code}
Value: JSON params
TTL: 300s
```

## 5. 熔断机制

### 5.1 触发条件

以下情况触发熔断，停止所有交易：

- 单日亏损 ≥ 5%
- 单周亏损 ≥ 10%
- 连续亏损天数 ≥ 3 天
- 系统异常（如行情数据异常）

### 5.2 熔断处理

```
1. 记录熔断事件到 exec_logs
2. 推送告警到 Kafka (futu.trade.events)
3. 设置 Redis 熔断标志
4. 拒绝所有新交易信号
5. 保留现有持仓
```

### 5.3 熔断恢复

熔断后需手动恢复：

```sql
UPDATE risk_rules
SET params_json = jsonb_set(params_json, '{circuit_breaker}', 'false')
WHERE rule_code = 'loss_limit';
```

或通过 API：

```bash
POST /api/risk/circuit-breaker/reset
```

## 6. 风控日志

### 6.1 日志格式

```json
{
  "timestamp": "2025-12-06T10:00:00Z",
  "level": "WARN",
  "module": "RiskController",
  "event": "RISK_REJECT",
  "symbol": "AAPL.US",
  "rule": "position_limit",
  "reason": "Single position exceeds 20%",
  "context": {
    "current_position": 0.18,
    "target_position": 0.25,
    "max_allowed": 0.20
  }
}
```

### 6.2 日志级别

| 级别 | 说明 |
|------|------|
| INFO | 正常风控通过 |
| WARN | 风控拒绝（非熔断） |
| ERROR | 熔断触发 |
| CRITICAL | 系统异常 |

## 7. 告警规则

### 7.1 Prometheus 告警

```yaml
groups:
  - name: risk_alerts
    rules:
      - alert: HighDailyLoss
        expr: daily_loss_ratio > 0.03
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "单日亏损超过 3%"

      - alert: CircuitBreakerTriggered
        expr: circuit_breaker_active == 1
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "熔断已触发"

      - alert: RiskRejectRate
        expr: rate(risk_reject_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "风控拒绝率过高"
```

## 8. 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2025-12-06 | 初始版本 |

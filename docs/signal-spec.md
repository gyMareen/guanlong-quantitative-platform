# 信号协议规范 (Signal Specification)

## 1. 概述

本文档定义了官龙量化一体化交易平台中使用的统一信号协议。所有信号（采集信号、量化信号）都应遵循此协议格式。

## 2. 信号来源

| 来源 | Topic | 描述 |
|------|-------|------|
| futu_rebalance | `futu.rebalance.raw` | 富途调仓历史采集 |
| quant_strategy | `futu.rebalance.qs` | 量化策略生成 |

## 3. 信号格式

### 3.1 完整信号结构

```json
{
  "symbol": "AAPL.US",
  "action": "BUY",
  "target_weight": 0.12,
  "target_position": null,
  "score": 1.23,
  "strategy": "multi_factor_v1",
  "strategy_version": "2025.12.06.1",
  "timestamp": "2025-12-06T10:00:00Z",
  "params_hash": "sha256:abc123...",
  "source": "futu_rebalance",
  "note": "Optional note"
}
```

### 3.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| symbol | string | 是 | 股票代码，含市场后缀（如 .US, .HK） |
| action | string | 是 | 动作类型：BUY/SELL/CLOSE/TARGET |
| target_weight | decimal | 否* | 目标权重（0-1），与 target_position 二选一 |
| target_position | integer | 否* | 目标持仓（股数），与 target_weight 二选一 |
| score | decimal | 否 | 量化评分，用于多信号融合 |
| strategy | string | 是 | 策略名称或来源标识 |
| strategy_version | string | 否 | 策略版本号 |
| timestamp | datetime | 是 | 信号生成时间（ISO 8601） |
| params_hash | string | 否 | 策略参数哈希，用于追溯 |
| source | string | 是 | 信号来源标识 |
| note | string | 否 | 备注信息 |

### 3.3 动作类型

| Action | 说明 | 处理逻辑 |
|--------|------|----------|
| BUY | 买入 | 增加持仓至目标 |
| SELL | 卖出 | 减少持仓 |
| CLOSE | 清仓 | 将持仓清零 |
| TARGET | 目标 | 调整至目标权重/数量 |

## 4. 采集信号示例

### 4.1 富途调仓信号

```json
{
  "symbol": "BABA.US",
  "action": "TARGET",
  "target_weight": 0.05,
  "strategy": "futu_rebalance",
  "strategy_version": "1.0",
  "timestamp": "2025-12-06T09:30:00Z",
  "source": "futu_rebalance",
  "note": "From Futu rebalance history"
}
```

### 4.2 采集原始数据

采集模块解析富途调仓历史后的原始数据结构：

```json
{
  "name": "阿里巴巴",
  "symbol": "BABA.US",
  "allocation_cur": 0.03,
  "allocation_tar": 0.05,
  "ref_price": 85.50,
  "date": "2025-12-06"
}
```

## 5. 量化信号示例

### 5.1 多因子策略信号

```json
{
  "symbol": "AAPL.US",
  "action": "BUY",
  "target_weight": 0.15,
  "score": 2.35,
  "strategy": "multi_factor_v1",
  "strategy_version": "2025.12.06.1",
  "timestamp": "2025-12-06T10:00:00Z",
  "params_hash": "sha256:a1b2c3d4e5f6...",
  "source": "quant_strategy"
}
```

### 5.2 趋势策略信号

```json
{
  "symbol": "TSLA.US",
  "action": "BUY",
  "target_weight": 0.08,
  "score": 1.5,
  "strategy": "ma_trend",
  "strategy_version": "1.2.0",
  "timestamp": "2025-12-06T10:00:00Z",
  "source": "quant_strategy"
}
```

### 5.3 清仓信号

```json
{
  "symbol": "NVDA.US",
  "action": "CLOSE",
  "strategy": "risk_control",
  "timestamp": "2025-12-06T14:00:00Z",
  "source": "quant_strategy",
  "note": "Stop loss triggered"
}
```

## 6. 信号优先级

当同一 symbol 有多个信号时，按以下优先级处理：

1. **时间优先**：最新信号优先
2. **来源优先**：量化信号 > 采集信号
3. **评分优先**：score 高的优先（同来源时）

## 7. 信号融合规则

### 7.1 多策略融合

当多个策略对同一 symbol 产生信号时：

```python
# 加权平均
final_weight = sum(signal.weight * signal.score for signal in signals) / sum(signal.score for signal in signals)
```

### 7.2 权重归一化

所有目标权重之和应归一化到 95%（预留 5% 现金）：

```python
total_weight = sum(weights)
normalized_weights = {s: w / total_weight * 0.95 for s, w in weights.items()}
```

## 8. 错误处理

### 8.1 无效信号

以下情况视为无效信号，应记录日志并丢弃：

- symbol 格式不正确
- action 不是有效值
- target_weight 超出 [0, 1] 范围
- timestamp 缺失或格式错误

### 8.2 信号重试

Kafka 消费失败时，最多重试 3 次，之后进入死信队列。

## 9. 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2025-12-06 | 初始版本 |

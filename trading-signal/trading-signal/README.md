# Trading Signal Collector

## 功能说明

自动采集富途App调仓历史数据，定时获取并存入数据库。

## 配置说明

### application.yml

```yaml
# 采集器配置
collector:
  enabled: true              # 是否启用采集器
  interval: 15000           # 采集间隔（毫秒）
  retry-count: 3            # 重试次数
  element-timeout: 10       # 元素查找超时（秒）

# Appium配置
appium:
  host: localhost
  port: 4723
  device-name: 23127PN0CC   # 设备ID
  app-package: cn.futu.trader
  app-activity: cn.futu.trader.main.activity.MainActivity
```

## 启动步骤

1. 启动Appium服务：
```bash
appium --use-plugins=inspector --allow-cors
```

2. 连接Android设备并打开富途App

3. 启动采集服务：
```bash
mvn spring-boot:run
```

## 数据流

```
富途App → Appium → RebalanceCollector → PostgreSQL + Kafka
```

采集的数据会：
- 存入 `rebalance_raw` 表
- 发送到 `futu.rebalance.raw` Kafka topic

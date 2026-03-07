# 运行手册 (Runbook)

## 1. 前置条件

### 1.1 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Docker | 24+ | 容器运行时 |
| Docker Compose | 2.x | 容器编排 |
| Java | 17+ | 运行 Java 服务 |
| Python | 3.10+ | 运行量化模块 |
| Node.js | 18+ | 运行前端 |

### 1.2 外部依赖

| 组件 | 端口 | 说明 |
|------|------|------|
| 雷电模拟器 | 5555 (ADB) | 运行富途 App |
| Appium Server | 4723 | 自动化驱动 |

## 2. 启动流程

### 2.1 启动基础设施

```bash
cd infra/compose
docker compose -f local.yml up -d

# 等待服务就绪
docker compose -f local.yml ps
```

预期输出：
```
NAME                STATUS              PORTS
postgres            running             0.0.0.0:5432->5432/tcp
kafka               running             0.0.0.0:9092->9092/tcp
zookeeper           running             2181/tcp
redis               running             0.0.0.0:6379->6379/tcp
```

### 2.2 初始化数据库

```bash
# 创建数据库
psql -U postgres -c "CREATE DATABASE guanlong;"

# 执行迁移脚本
psql -U postgres -d guanlong -f infra/sql/init.sql
```

### 2.3 创建 Kafka Topics

```bash
# 进入 Kafka 容器
docker exec -it kafka bash

# 创建 topics
kafka-topics --create --topic futu.rebalance.raw --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic futu.rebalance.qs --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic futu.trade.events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# 验证
kafka-topics --list --bootstrap-server localhost:9092
```

### 2.4 启动交易中台

```bash
cd trading-platform
mvn spring-boot:run
```

### 2.5 启动采集服务（可选）

```bash
# 前置：启动雷电模拟器和 Appium Server
# 1. 打开雷电模拟器
# 2. 打开富途 App
# 3. 启动 Appium Server: appium

cd trading-signal
mvn spring-boot:run
```

### 2.6 启动量化服务

```bash
cd quant-research
pip install -r requirements.txt
python src/signals/producer.py
```

### 2.7 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 3. 验证检查

### 3.1 基础设施检查

```bash
# PostgreSQL
psql -U postgres -d guanlong -c "SELECT 1;"

# Kafka
docker exec -it kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Redis
redis-cli ping
```

### 3.2 服务健康检查

```bash
# 交易中台
curl http://localhost:8080/actuator/health

# 前端
curl http://localhost:5173
```

### 3.3 Kafka 消息检查

```bash
# 消费采集信号
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic futu.rebalance.raw \
  --from-beginning

# 消费量化信号
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic futu.rebalance.qs \
  --from-beginning

# 消费交易事件
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic futu.trade.events \
  --from-beginning
```

## 4. 日常运维

### 4.1 查看日志

```bash
# Docker 服务日志
docker compose -f infra/compose/local.yml logs -f trading-platform

# 应用日志
tail -f trading-platform/logs/application.log
```

### 4.2 监控指标

访问 Grafana: http://localhost:3000

关键指标：
- `kafka_consumer_lag`: Kafka 消费延迟
- `order_count`: 订单数量
- `trade_amount`: 交易金额
- `daily_pnl`: 日盈亏

### 4.3 数据库维护

```bash
# 备份数据库
pg_dump -U postgres guanlong > backup_$(date +%Y%m%d).sql

# 清理旧日志（保留 30 天）
psql -U postgres -d guanlong -c "DELETE FROM exec_logs WHERE ts < NOW() - INTERVAL '30 days';"
```

## 5. 故障排查

### 5.1 Appium 定位失败

**症状**: 采集服务报错 "Element not found"

**排查步骤**:
1. 检查雷电模拟器是否运行
2. 检查富途 App 是否打开
3. 检查 Appium Server 是否运行
4. 尝试重启富途 App

```bash
# 重启 Appium
pkill -f appium
appium &

# 检查连接
adb devices
```

### 5.2 Kafka 连接失败

**症状**: 服务启动报错 "Connection to kafka:9092 failed"

**排查步骤**:
1. 检查 Kafka 容器状态
2. 检查网络配置
3. 检查 Kafka 日志

```bash
# 检查容器状态
docker compose -f infra/compose/local.yml ps kafka

# 查看 Kafka 日志
docker compose -f infra/compose/local.yml logs kafka

# 重启 Kafka
docker compose -f infra/compose/local.yml restart kafka
```

### 5.3 LongPort 鉴权失败

**症状**: 下单报错 "Authentication failed"

**排查步骤**:
1. 检查 keysss.txt 文件是否存在
2. 检查 API Key 是否过期
3. 检查账户权限

```bash
# 检查配置文件
cat /path/to/keysss.txt

# 测试连接
curl -X GET "https://openapi.longportapp.com/v1/asset/account" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 5.4 数据库连接失败

**症状**: 服务启动报错 "Connection to postgresql refused"

**排查步骤**:
1. 检查 PostgreSQL 容器状态
2. 检查连接配置
3. 检查数据库是否存在

```bash
# 检查容器
docker compose -f infra/compose/local.yml ps postgres

# 测试连接
psql -h localhost -U postgres -d guanlong -c "SELECT 1;"
```

## 6. 停止流程

### 6.1 停止应用服务

```bash
# 停止前端
# Ctrl+C

# 停止量化服务
# Ctrl+C

# 停止采集服务
# Ctrl+C

# 停止交易中台
# Ctrl+C
```

### 6.2 停止基础设施

```bash
cd infra/compose
docker compose -f local.yml down

# 如需清理数据
docker compose -f local.yml down -v
```

## 7. 应急处理

### 7.1 紧急停止交易

```bash
# 设置熔断标志
redis-cli SET circuit_breaker:active 1

# 或通过 API
curl -X POST http://localhost:8080/api/risk/circuit-breaker/trigger
```

### 7.2 清理卡住的订单

```sql
-- 查看卡住的订单
SELECT * FROM orders WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL '1 hour';

-- 取消订单
UPDATE orders SET status = 'CANCELLED', error_msg = 'Manual cancel' WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL '1 hour';
```

### 7.3 恢复熔断

```bash
# 清除熔断标志
redis-cli DEL circuit_breaker:active

# 或通过 API
curl -X POST http://localhost:8080/api/risk/circuit-breaker/reset
```

## 8. 联系方式

| 角色 | 联系方式 |
|------|----------|
| 系统管理员 | admin@example.com |
| 开发团队 | dev@example.com |

## 9. 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2025-12-06 | 初始版本 |

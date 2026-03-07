# 官龙量化一体化交易平台（订单采集 / 量化策略 / 交易中台）

## 1. 背景与目标

结合《多策略协同量化交易系统》（量化模块）与《原型需求文档》（富途调仓采集与 LongPort 交易），构建统一的本地化交易平台。

### 目标
- **订单采集**：基于 Appium（Java）从雷电模拟器的富途 App 抓取调仓历史，生成交易信号
- **量化策略**：沿用多策略 Python 研究/回测/信号生成模块，输出统一信号协议
- **交易中台**：Java 栈（Spring Boot/Kafka/PostgreSQL）消费量化与采集信号，统一风控与下单执行（可对接 LongPort/券商）
- **基础设施**：本地 Docker 化（Kafka/PostgreSQL/Redis/监控）

## 2. 项目结构

```
guanlong-quantitative-platform/
├── quant-research/                 # Python 研究与回测（多策略协同量化交易系统）
│   ├── src/
│   │   ├── data/                   # 数据接入与清洗
│   │   ├── factors/                # 因子库
│   │   ├── strategies/             # 策略实现
│   │   ├── backtest/               # Backtrader 封装
│   │   ├── portfolio/              # 组合与多策略协同
│   │   └── signals/                # 统一信号协议输出
│   ├── tests/                      # 单测与回测快照
│   ├── notebooks/                  # Jupyter 研究
│   └── requirements.txt
│
├── trading-signal/                 # Java 交易信息获取（Appium 采集富途调仓历史）
│   ├── src/main/java/com/guanlong/signal/
│   │   ├── appium/                 # Appium 驱动
│   │   ├── parser/                 # 数据解析
│   │   ├── kafka/                  # Kafka 生产者
│   │   └── config/                 # 配置
│   └── pom.xml
│
├── trading-platform/               # Java 交易中台（风控、下单、订单生命周期）
│   ├── src/main/java/com/guanlong/trading/
│   │   ├── kafka/                  # Kafka 消费与生产
│   │   ├── domain/                 # 领域模型
│   │   ├── service/                # 业务服务
│   │   ├── infra/                  # 基础设施
│   │   └── controller/             # REST 接口
│   └── pom.xml
│
├── frontend/                       # Vue 3 前端管理界面
│   └── src/
│       ├── views/                  # 页面组件
│       ├── api/                    # API 接口
│       └── store/                  # 状态管理
│
├── infra/                          # 基础设施配置
│   ├── compose/                    # Docker Compose 文件
│   ├── sql/                        # 数据库迁移脚本
│   └── config/                     # 配置文件
│
├── docs/                           # 文档
│   ├── signal-spec.md              # 信号协议规范
│   ├── risk-rules.md               # 风险约束规则
│   └── runbook.md                  # 运行手册
│
├── CLAUDE.md                       # 自动编程 Agent 指南
├── task.json                       # 任务列表
├── progress.txt                    # 进度记录
├── architecture.md                 # 架构说明
└── README.md                       # 本文件
```

## 3. 子系统职责与技术栈

### 3.1 订单采集（trading-signal）
- **技术**：Java 17 + Appium Client
- **功能**：连接宿主 Appium Server / 雷电模拟器 ADB，读取富途"调仓历史"，解析行数据 → PostgreSQL + Kafka
- **输出**：Kafka Topic `futu.rebalance.raw`

### 3.2 量化策略（quant-research）
- **技术**：Python 3.10+，Backtrader/pandas/numpy
- **功能**：输出统一信号 JSON，发布 Kafka，写 PostgreSQL
- **输出**：Kafka Topic `futu.rebalance.qs`

### 3.3 交易中台（trading-platform）
- **技术**：Java 17 + Spring Boot 3 + Spring Kafka + PostgreSQL + Redis
- **功能**：消费 Kafka/DB 信号，统一风控、订单路由、状态跟踪，支持 LongPort 接口对接
- **输出**：Kafka Topic `futu.trade.events`

### 3.4 前端管理（frontend）
- **技术**：Vue 3 + Vite + TypeScript + Element Plus
- **功能**：策略监控、订单管理、持仓查看、风控配置

### 3.5 监控（可选）
- **技术**：Prometheus + Grafana（可选 Loki/ELK）
- **功能**：Kafka/应用指标与日志

## 4. 策略选型

### 趋势类
- MA（移动平均）
- Breakout（突破）

### 均值回归
- Bollinger Bands
- OU 简化

### 多因子评分
- Momentum/Vol/MR 融合
- 得分 → 目标权重

### 组合层
- Score 融合 → 目标权重
- 风险约束（单票/组合上限）

## 5. 业务流程与数据流

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   富途 App       │     │  Python 量化     │     │   交易中台       │
│  (雷电模拟器)    │     │   (策略引擎)     │     │  (Spring Boot)  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         ▼                       ▼                       │
┌─────────────────┐     ┌─────────────────┐              │
│  Appium 采集     │     │  信号生成        │              │
│  (trading-signal)│     │  (quant-research)│             │
└────────┬────────┘     └────────┬────────┘              │
         │                       │                       │
         ▼                       ▼                       │
┌─────────────────────────────────────────┐              │
│              Kafka Topics                │              │
│  • futu.rebalance.raw (采集信号)         │              │
│  • futu.rebalance.qs  (量化信号)         │──────────────┘
└─────────────────────────────────────────┘
                                                 │
                                                 ▼
                                         ┌─────────────────┐
                                         │   风控 & 下单    │
                                         │  (LongPort API) │
                                         └────────┬────────┘
                                                  │
                                                  ▼
                                         ┌─────────────────┐
                                         │  PostgreSQL     │
                                         │  • orders       │
                                         │  • positions    │
                                         │  • exec_logs    │
                                         └─────────────────┘
```

## 6. 数据与接口规范

### 6.1 PostgreSQL 表

#### rebalance_raw（采集原始数据）
```sql
CREATE TABLE rebalance_raw (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50),
    name VARCHAR(100),
    allocation_cur DECIMAL(10,4),
    allocation_tar DECIMAL(10,4),
    symbol VARCHAR(20),
    ref_price DECIMAL(10,4),
    payload_json JSONB
);
```

#### rebalance_qs（量化信号）
```sql
CREATE TABLE rebalance_qs (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    strategy VARCHAR(100),
    strategy_version VARCHAR(50),
    symbol VARCHAR(20),
    target_weight DECIMAL(10,4),
    action VARCHAR(20),
    price DECIMAL(10,4),
    note TEXT,
    payload_json JSONB
);
```

#### orders（订单）
```sql
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50),
    symbol VARCHAR(20),
    side VARCHAR(10),
    price DECIMAL(10,4),
    qty INTEGER,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    error_msg TEXT,
    broker VARCHAR(50)
);
```

#### positions_snapshot（持仓快照）
```sql
CREATE TABLE positions_snapshot (
    ts TIMESTAMP,
    symbol VARCHAR(20),
    qty INTEGER,
    mkt_price DECIMAL(10,4),
    market VARCHAR(20),
    account_id VARCHAR(50)
);
```

#### exec_logs（执行日志）
```sql
CREATE TABLE exec_logs (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(10),
    module VARCHAR(50),
    message TEXT,
    context_json JSONB
);
```

### 6.2 Kafka Topics

| Topic | 描述 | 生产者 | 消费者 |
|-------|------|--------|--------|
| `futu.rebalance.raw` | 采集输出（富途调仓历史解析结果） | trading-signal | trading-platform |
| `futu.rebalance.qs` | 量化/聚合信号输出 | quant-research | trading-platform |
| `futu.trade.events` | 交易回报/告警/风控拦截事件 | trading-platform | 监控/告警 |

### 6.3 统一信号协议

```json
{
  "symbol": "AAPL.US",
  "action": "BUY",
  "target_weight": 0.12,
  "score": 1.23,
  "strategy": "multi_factor_v1",
  "strategy_version": "2025.12.06.1",
  "timestamp": "2025-12-06T10:00:00Z",
  "params_hash": "sha256:...",
  "source": "futu_rebalance"
}
```

## 7. 环境要求

- JDK 17+
- Python 3.10+
- Node.js 18+
- PostgreSQL 14+
- Redis 6+
- Kafka 3.x
- Docker & Docker Compose
- 雷电模拟器（可选，用于采集）
- Appium Server（可选，用于采集）

## 8. 快速开始

### 8.1 启动基础设施

```bash
cd infra/compose
docker compose -f local.yml up -d
```

### 8.2 初始化数据库

```bash
psql -U postgres -f infra/sql/init.sql
```

### 8.3 启动后端服务

```bash
# 交易中台
cd trading-platform
mvn spring-boot:run

# 采集服务（可选）
cd trading-signal
mvn spring-boot:run
```

### 8.4 启动量化服务

```bash
cd quant-research
pip install -r requirements.txt
python src/signals/producer.py
```

### 8.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 9. 风险与待办

### 风险
- UI 变更风险：元素定位失效需快速调整
- 订单时效：未实现挂单超时自动取消
- 配置化不足：价差/最小交易额硬编码需外置配置
- 多市场/多账户：当前默认美股 `.US`

### 待办
- [ ] 视觉/多特征定位增强
- [ ] 挂单超时自动取消
- [ ] 参数外部化配置
- [ ] 多市场/多账户支持
- [ ] 完善监控告警

## 10. License

MIT

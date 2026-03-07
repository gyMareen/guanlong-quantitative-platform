# 官龙量化一体化交易平台 - 自动编程 Agent 指南

## 项目概述

官龙量化一体化交易平台是一个集成订单采集、量化策略、交易中台的本地化交易系统。

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.3
- Spring Kafka
- Spring Security + JWT
- MyBatis-Plus 3.5.5
- PostgreSQL 14+
- Redis 6+
- Kafka 3.x

### 量化
- Python 3.10+
- Backtrader
- pandas / numpy
- confluent-kafka-python

### 前端
- Vue 3.4
- Vite 5
- TypeScript 5
- Element Plus 2.5

### 基础设施
- Docker & Docker Compose
- Prometheus + Grafana（监控）
- Appium（采集）

## 开发流程

### 1. 初始化环境

运行初始化脚本，安装依赖并启动开发服务器：

```bash
./init.sh
```

或手动执行：

```bash
# 基础设施
cd infra/compose
docker compose -f local.yml up -d

# 交易中台
cd trading-platform
mvn clean install -DskipTests

# 量化模块
cd quant-research
pip install -r requirements.txt

# 前端
cd frontend
npm install
```

### 2. 选择任务

读取 `task.json`，选择一个 `passes: false` 的任务：

- 任务按优先级排序，优先选择 `priority` 值小的任务
- 确保任务的 `dependencies` 中的任务都已通过
- 记录任务 ID 用于后续更新

### 3. 实现任务

按照任务描述的步骤实现功能：

- **trading-platform**：Java 后端代码放在 `trading-platform/src/main/java/com/guanlong/trading/`
- **trading-signal**：采集代码放在 `trading-signal/src/main/java/com/guanlong/signal/`
- **quant-research**：Python 代码放在 `quant-research/src/`
- **frontend**：前端代码放在 `frontend/src/`
- **infra**：基础设施配置放在 `infra/`
- **docs**：文档放在 `docs/`

### 4. 测试验证

运行以下命令确保代码正确：

```bash
# Java 项目编译检查
cd trading-platform && mvn compile
cd trading-signal && mvn compile

# Python 代码检查
cd quant-research
black --check src/
ruff check src/

# 前端代码检查
cd frontend
npm run lint
npm run build
```

### 5. 更新进度

将工作记录到 `progress.txt`：

```text
## [任务ID] 任务名称 - YYYY-MM-DD HH:mm

### 完成内容
- 具体完成的工作项

### 修改文件
- 文件路径列表

### 备注
- 任何需要记录的信息
```

### 6. 提交更改

将 `task.json` 中对应任务的 `passes` 更新为 `true`，然后一次性提交所有更改：

```bash
git add .
git commit -m "feat: 完成任务ID - 任务描述"
```

## 项目结构

```
guanlong-quantitative-platform/
├── quant-research/                 # Python 量化研究
│   ├── src/
│   │   ├── data/                   # 数据接入
│   │   ├── factors/                # 因子库
│   │   ├── strategies/             # 策略实现
│   │   ├── backtest/               # 回测框架
│   │   ├── portfolio/              # 组合管理
│   │   └── signals/                # 信号输出
│   ├── tests/
│   ├── notebooks/
│   └── requirements.txt
│
├── trading-signal/                 # Java 采集服务
│   └── src/main/java/com/guanlong/signal/
│       ├── appium/
│       ├── parser/
│       ├── kafka/
│       └── config/
│
├── trading-platform/               # Java 交易中台
│   └── src/main/java/com/guanlong/trading/
│       ├── kafka/                  # Kafka 消费/生产
│       ├── domain/                 # 领域模型
│       ├── service/                # 业务服务
│       ├── infra/                  # 基础设施
│       │   ├── broker/             # 券商适配
│       │   └── persistence/        # 持久化
│       └── controller/             # REST 接口
│
├── frontend/                       # Vue 3 前端
│   └── src/
│       ├── views/
│       ├── api/
│       └── store/
│
├── infra/                          # 基础设施
│   ├── compose/                    # Docker Compose
│   ├── sql/                        # SQL 脚本
│   └── config/                     # 配置文件
│
├── docs/                           # 文档
│   ├── signal-spec.md
│   ├── risk-rules.md
│   └── runbook.md
│
├── CLAUDE.md
├── task.json
├── progress.txt
├── architecture.md
└── README.md
```

## 核心业务规则

### 信号处理
- 输入优先级：Kafka `futu.rebalance.qs` > `futu.rebalance.raw` > PostgreSQL 最新批次
- 信号归并：同一 symbol 多策略冲突仲裁
- 差值计算：目标仓位 - 当前持仓 → 交易指令

### 风控规则
- 单票最大仓位：≤ 总资产 20%
- 组合最大仓位：≤ 总资产 95%
- 单日最大亏损：≤ 总资产 5%
- 价格偏离保护：≤ 4%
- 最小交易额：≥ 20 美元
- 目标为 0：执行清仓

### 订单执行
- 订单类型：限价单 (LO)
- 价格偏差：≤ 4%
- 超时处理：待实现自动取消

## 代码规范

### Java (Spring Boot)
- 使用 Lombok 减少样板代码
- 统一使用 `ApiResponse` 封装响应
- 异常使用 `BusinessException` 抛出
- 领域模型放在 `domain` 包
- 券商适配使用策略模式

### Python (量化)
- 使用 Black 格式化
- 使用 isort 排序 import
- 使用 ruff 检查
- 回测必须包含手续费和滑点
- 信号输出遵循 `docs/signal-spec.md`

### 前端 (Vue/TypeScript)
- 使用 Composition API
- 使用 Pinia 状态管理
- 组件使用 PascalCase 命名
- 样式使用 SCSS

## Kafka Topics

| Topic | 方向 | 描述 |
|-------|------|------|
| `futu.rebalance.raw` | 生产 | 采集信号 |
| `futu.rebalance.qs` | 生产 | 量化信号 |
| `futu.trade.events` | 生产 | 交易事件 |

## 注意事项

1. 每次只处理一个任务
2. 提交前确保编译通过
3. 保持代码风格一致
4. 更新 progress.txt 记录工作内容
5. 量化代码需包含单元测试
6. 敏感配置使用环境变量

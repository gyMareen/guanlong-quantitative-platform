// 通用分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 持仓
export interface Position {
  id: number
  symbol: string
  qty: number
  availableQty: number
  costPrice: number
  mktPrice: number
  marketValue: number
  pnl: number
  pnlRatio: number
  market: string
  accountId: string
  updatedAt: string
}

// 账户余额
export interface BalanceInfo {
  balance: number
}

// 账户概览
export interface AccountOverview {
  totalEquity: number
  positionValue: number
  cashBalance: number
  totalPnL: number
  positionRatio: number
  positionCount: number
}

// 持仓分布
export interface PositionDistribution {
  symbol: string
  marketValue: number
  weight: number
}

// 订单
export interface Order {
  id: number
  batchId: string
  signalId: number
  symbol: string
  side: string
  orderType: string
  price: number
  qty: number
  filledQty: number
  avgPrice: number
  status: string
  broker: string
  brokerOrderId: string
  errorMsg: string
  createdAt: string
  updatedAt: string
}

// 订单统计
export interface OrderStatistics {
  todayOrders: number
  pendingOrders: number
  todayFilled: number
  todayRejected: number
}

// 信号
export interface Signal {
  id: number
  symbol: string
  action: string
  targetWeight: number
  targetPosition: number
  score: number
  price: number
  strategy: string
  strategyVersion: string
  paramsHash: string
  source: string
  note: string
  batchId: string
  timestamp: string
  createdAt: string
}

// 信号统计
export interface SignalStatistics {
  todayCount: number
  executedCount: number
  pendingCount: number
  rejectedCount: number
}

// 风控规则
export interface RiskRule {
  id: number
  ruleCode: string
  ruleName: string
  description: string
  paramsJson: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// 风控配置
export interface RiskConfig {
  maxSinglePosition: number
  maxTotalPosition: number
  maxDailyLoss: number
  maxWeeklyLoss: number
  maxPriceDeviation: number
  minTradeAmount: number
}

// 熔断状态
export interface CircuitBreakerStatus {
  active: boolean
  reason: string
  triggeredAt: number
}

// 订单风险检查请求
export interface OrderRiskCheck {
  symbol: string
  orderPrice: number
  orderQty: number
  marketPrice: number
  targetWeight: number
  totalEquity: number
}

// 风险检查结果
export interface RiskCheckResult {
  passed: boolean
  message: string
}

// 仪表盘概览
export interface DashboardOverview {
  totalEquity: number
  positionValue: number
  cashBalance: number
  totalPnL: number
  positionCount: number
  todayOrders: number
  todaySignals: number
  pendingOrders: number
}

// 账户统计
export interface AccountStats {
  totalEquity: number
  cashBalance: number
  positionValue: number
  cashRatio: number
  positionRatio: number
  totalPnL: number
  avgPnLRatio: number
  positionCount: number
}

// 今日交易统计
export interface TodayStats {
  totalOrders: number
  filledOrders: number
  pendingOrders: number
  rejectedOrders: number
  totalSignals: number
  fillRate: number
}

// 持仓汇总
export interface PositionSummary {
  positionCount: number
  totalValue: number
  totalPnL: number
  pnlRatio: number
  maxPositionRatio: number
}

// 交易时间配置
export interface TradingHours {
  morningOpen: string
  morningClose: string
  afternoonOpen: string
  afternoonClose: string
  tradingEnabled: boolean
}

// 版本信息
export interface VersionInfo {
  version: string
  buildDate: string
  framework: string
  javaVersion: string
}

// 系统设置
export interface SystemSettings {
  activeProfile: string
  serverPort: string
  riskConfig: Record<string, unknown>
}

import { request } from '@/utils/request'
import type {
  RiskRule,
  RiskConfig,
  CircuitBreakerStatus,
  OrderRiskCheck,
  RiskCheckResult
} from './types'

// 获取所有风控规则
export const getRiskRules = (): Promise<RiskRule[]> => {
  return request.get('/risk/rules')
}

// 获取启用的风控规则
export const getEnabledRules = (): Promise<RiskRule[]> => {
  return request.get('/risk/rules/enabled')
}

// 获取风控规则详情
export const getRiskRule = (id: number): Promise<RiskRule> => {
  return request.get(`/risk/rules/${id}`)
}

// 获取当前风控配置
export const getRiskConfig = (): Promise<RiskConfig> => {
  return request.get('/risk/config')
}

// 更新风控规则
export const updateRiskRule = (id: number, rule: RiskRule): Promise<RiskRule> => {
  return request.put(`/risk/rules/${id}`, rule)
}

// 启用/禁用风控规则
export const toggleRule = (id: number, enabled: boolean): Promise<void> => {
  return request.put(`/risk/rules/${id}/toggle`, null, { params: { enabled } })
}

// 更新规则参数
export const updateRuleParams = (id: number, paramsJson: string): Promise<void> => {
  return request.put(`/risk/rules/${id}/params`, paramsJson, {
    headers: { 'Content-Type': 'application/json' }
  })
}

// 触发熔断
export const triggerCircuitBreaker = (reason: string): Promise<void> => {
  return request.post('/risk/circuit-breaker/trigger', null, { params: { reason } })
}

// 重置熔断
export const resetCircuitBreaker = (): Promise<void> => {
  return request.post('/risk/circuit-breaker/reset')
}

// 获取熔断状态
export const getCircuitBreakerStatus = (): Promise<CircuitBreakerStatus> => {
  return request.get('/risk/circuit-breaker/status')
}

// 检查订单风险
export const checkOrderRisk = (data: OrderRiskCheck): Promise<RiskCheckResult> => {
  return request.post('/risk/check', data)
}

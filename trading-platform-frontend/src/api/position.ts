import { request } from '@/utils/request'
import type { Position, BalanceInfo, AccountOverview, PositionDistribution } from './types'

// 获取当前持仓列表
export const getPositions = (): Promise<Position[]> => {
  return request.get('/positions')
}

// 获取账户余额
export const getBalance = (): Promise<BalanceInfo> => {
  return request.get('/positions/balance')
}

// 获取账户概览
export const getAccountOverview = (): Promise<AccountOverview> => {
  return request.get('/positions/overview')
}

// 获取持仓分布
export const getPositionDistribution = (): Promise<PositionDistribution[]> => {
  return request.get('/positions/distribution')
}

// 获取单只持仓详情
export const getPosition = (symbol: string): Promise<Position> => {
  return request.get(`/positions/${symbol}`)
}

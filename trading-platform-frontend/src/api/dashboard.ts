import { request } from '@/utils/request'
import type {
  DashboardOverview,
  AccountStats,
  TodayStats,
  PositionSummary
} from './types'

// 获取仪表盘概览数据
export const getOverview = (): Promise<DashboardOverview> => {
  return request.get('/dashboard/overview')
}

// 获取账户统计
export const getAccountStats = (): Promise<AccountStats> => {
  return request.get('/dashboard/account-stats')
}

// 获取今日交易统计
export const getTodayStats = (): Promise<TodayStats> => {
  return request.get('/dashboard/today-stats')
}

// 获取持仓汇总
export const getPositionSummary = (): Promise<PositionSummary> => {
  return request.get('/dashboard/position-summary')
}

import { request } from '@/utils/request'
import type { TradingHours, VersionInfo, SystemSettings } from './types'

// 获取系统配置
export const getSettings = (): Promise<SystemSettings> => {
  return request.get('/settings')
}

// 获取风控配置
export const getRiskSettings = (): Promise<Record<string, unknown>> => {
  return request.get('/settings/risk')
}

// 更新风控配置
export const updateRiskSettings = (settings: Record<string, unknown>): Promise<void> => {
  return request.put('/settings/risk', settings)
}

// 获取交易时间配置
export const getTradingHours = (): Promise<TradingHours> => {
  return request.get('/settings/trading-hours')
}

// 获取版本信息
export const getVersion = (): Promise<VersionInfo> => {
  return request.get('/settings/version')
}

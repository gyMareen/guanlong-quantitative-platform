import { request } from '@/utils/request'
import type { Signal, SignalStatistics, PageResponse } from './types'

// 分页查询信号
export interface SignalQueryParams {
  page?: number
  size?: number
  symbol?: string
  strategy?: string
  source?: string
  startDate?: string
  endDate?: string
}

export const getSignals = (params: SignalQueryParams): Promise<PageResponse<Signal>> => {
  return request.get('/signals', { params })
}

// 获取今日信号
export const getTodaySignals = (): Promise<Signal[]> => {
  return request.get('/signals/today')
}

// 获取信号统计
export const getSignalStatistics = (): Promise<SignalStatistics> => {
  return request.get('/signals/statistics')
}

// 获取信号详情
export const getSignal = (id: number): Promise<Signal> => {
  return request.get(`/signals/${id}`)
}

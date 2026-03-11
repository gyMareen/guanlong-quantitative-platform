import { request } from '@/utils/request'
import type { Order, Signal, OrderStatistics, PageResponse } from './types'

// 处理信号并生成订单
export const processSignals = (signals: Signal[]): Promise<Order[]> => {
  return request.post('/trading/signals/process', signals)
}

// 执行订单
export const executeOrders = (orders: Order[]): Promise<void> => {
  return request.post('/trading/orders/execute', orders)
}

// 获取活跃订单
export const getActiveOrders = (): Promise<Order[]> => {
  return request.get('/trading/orders/active')
}

// 分页查询订单
export interface OrderQueryParams {
  page?: number
  size?: number
  symbol?: string
  status?: string
  startDate?: string
  endDate?: string
}

export const getOrders = (params: OrderQueryParams): Promise<PageResponse<Order>> => {
  return request.get('/trading/orders', { params })
}

// 获取订单详情
export const getOrder = (id: number): Promise<Order> => {
  return request.get(`/trading/orders/${id}`)
}

// 取消订单
export const cancelOrder = (id: number): Promise<void> => {
  return request.post(`/trading/orders/${id}/cancel`)
}

// 获取订单统计
export const getOrderStatistics = (): Promise<OrderStatistics> => {
  return request.get('/trading/orders/statistics')
}

<template>
  <div class="order-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-button type="primary" @click="refreshOrders">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="股票代码">
          <el-input v-model="searchForm.symbol" placeholder="请输入股票代码" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待处理" value="PENDING" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="部分成交" value="PARTIAL_FILLED" />
            <el-option label="已成交" value="FILLED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 订单列表 -->
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="id" label="订单ID" width="80" />
        <el-table-column prop="symbol" label="股票代码" width="120" />
        <el-table-column prop="side" label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.side === 'BUY' ? 'success' : 'danger'" size="small">
              {{ row.side === 'BUY' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderType" label="类型" width="80">
          <template #default="{ row }">
            {{ row.orderType === 'MARKET' ? '市价' : '限价' }}
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            {{ row.price ? `$${row.price.toFixed(4)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="filledQty" label="已成交" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="150">
          <template #default="{ row }">
            <span v-if="row.errorMsg" class="error-text">{{ row.errorMsg }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="['PENDING', 'SUBMITTED'].includes(row.status)"
              type="danger"
              size="small"
              @click="cancelOrder(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getOrders, cancelOrder as cancelOrderApi } from '@/api/trading'
import type { Order } from '@/api/types'

const loading = ref(false)
const orders = ref<Order[]>([])

const searchForm = reactive({
  symbol: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fetchOrders = async () => {
  loading.value = true
  try {
    // 调用 API 获取订单数据
    const params = {
      page: pagination.page,
      size: pagination.size,
      symbol: searchForm.symbol || undefined,
      status: searchForm.status || undefined,
      startDate: searchForm.dateRange?.[0] ? dayjs(searchForm.dateRange[0]).format('YYYY-MM-DD HH:mm:ss') : undefined,
      endDate: searchForm.dateRange?.[1] ? dayjs(searchForm.dateRange[1]).format('YYYY-MM-DD HH:mm:ss') : undefined
    }

    const res = await getOrders(params)
    orders.value = res.records
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('获取订单列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const refreshOrders = () => {
  pagination.page = 1
  fetchOrders()
}

const handleSearch = () => {
  pagination.page = 1
  fetchOrders()
}

const resetSearch = () => {
  searchForm.symbol = ''
  searchForm.status = ''
  searchForm.dateRange = []
  refreshOrders()
}

const handleSizeChange = () => {
  fetchOrders()
}

const handlePageChange = () => {
  fetchOrders()
}

const cancelOrder = async (order: Order) => {
  try {
    await ElMessageBox.confirm(`确认取消订单 #${order.id}？`, '提示', {
      type: 'warning'
    })
    // 调用取消订单 API
    await cancelOrderApi(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error: any) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
    // 用户取消不处理
  }
}

const getStatusType = (status: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const types: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    PENDING: 'info',
    SUBMITTED: 'warning',
    PARTIAL_FILLED: 'warning',
    FILLED: 'success',
    CANCELLED: 'info',
    REJECTED: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    PENDING: '待处理',
    SUBMITTED: '已提交',
    PARTIAL_FILLED: '部分成交',
    FILLED: '已成交',
    CANCELLED: '已取消',
    REJECTED: '已拒绝'
  }
  return texts[status] || status
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped lang="scss">
.order-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .error-text {
    color: #f56c6c;
    font-size: 12px;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

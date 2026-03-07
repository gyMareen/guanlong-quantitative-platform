<template>
  <div class="signal-page">
    <el-row :gutter="20">
      <!-- 信号统计 -->
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #409eff">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ signalStats.todayCount }}</div>
              <div class="stat-label">今日信号</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon :size="28"><Check /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ signalStats.executedCount }}</div>
              <div class="stat-label">已执行</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon :size="28"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ signalStats.pendingCount }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon :size="28"><Close /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ signalStats.rejectedCount }}</div>
              <div class="stat-label">已拒绝</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 信号列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>信号列表</span>
          <div class="header-actions">
            <el-select v-model="filterStrategy" placeholder="策略筛选" clearable style="width: 150px; margin-right: 10px">
              <el-option label="MA趋势" value="ma_trend" />
              <el-option label="突破策略" value="breakout" />
              <el-option label="布林带" value="bollinger" />
              <el-option label="多因子" value="multi_factor" />
            </el-select>
            <el-button type="primary" @click="refreshSignals">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="signals" v-loading="loading" stripe>
        <el-table-column prop="timestamp" label="时间" width="160" />
        <el-table-column prop="symbol" label="股票代码" width="120" />
        <el-table-column prop="action" label="动作" width="80">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetWeight" label="目标权重" width="100">
          <template #default="{ row }">
            {{ (row.targetWeight * 100).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="80">
          <template #default="{ row }">
            <span :class="{ profit: row.score > 0, loss: row.score < 0 }">
              {{ row.score?.toFixed(2) || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="strategy" label="策略" width="120" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="150" />
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[20, 50, 100]"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

interface Signal {
  timestamp: string
  symbol: string
  action: string
  targetWeight: number
  score: number
  strategy: string
  source: string
  status: string
  note: string
}

const loading = ref(false)
const filterStrategy = ref('')
const signals = ref<Signal[]>([])

const signalStats = reactive({
  todayCount: 45,
  executedCount: 38,
  pendingCount: 5,
  rejectedCount: 2
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 45
})

const fetchSignals = async () => {
  loading.value = true
  try {
    // TODO: 调用 API
    signals.value = [
      { timestamp: '2026-03-08 10:30:00', symbol: 'AAPL.US', action: 'BUY', targetWeight: 0.15, score: 0.85, strategy: 'multi_factor', source: 'quant', status: 'EXECUTED', note: '' },
      { timestamp: '2026-03-08 10:25:00', symbol: 'TSLA.US', action: 'SELL', targetWeight: 0.05, score: -0.45, strategy: 'ma_trend', source: 'quant', status: 'PENDING', note: '等待执行' },
      { timestamp: '2026-03-08 10:20:00', symbol: 'MSFT.US', action: 'BUY', targetWeight: 0.10, score: 0.62, strategy: 'breakout', source: 'quant', status: 'EXECUTED', note: '' },
      { timestamp: '2026-03-08 10:15:00', symbol: 'GOOGL.US', action: 'CLOSE', targetWeight: 0, score: -0.75, strategy: 'bollinger', source: 'quant', status: 'REJECTED', note: '风控拒绝：价格偏离过大' }
    ]
  } catch {
    ElMessage.error('获取信号列表失败')
  } finally {
    loading.value = false
  }
}

const refreshSignals = () => {
  pagination.page = 1
  fetchSignals()
}

const getActionType = (action: string) => {
  const types: Record<string, string> = {
    BUY: 'success',
    SELL: 'danger',
    CLOSE: 'warning',
    TARGET: 'info'
  }
  return types[action] || 'info'
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    PENDING: 'warning',
    EXECUTED: 'success',
    REJECTED: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    PENDING: '待处理',
    EXECUTED: '已执行',
    REJECTED: '已拒绝'
  }
  return texts[status] || status
}

onMounted(() => {
  fetchSignals()
})
</script>

<style scoped lang="scss">
.signal-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      align-items: center;
    }
  }

  .stat-card {
    display: flex;
    align-items: center;
    padding: 10px 0;

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .stat-info {
      margin-left: 16px;

      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #303133;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .profit {
    color: #67c23a;
  }

  .loss {
    color: #f56c6c;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

<template>
  <div class="position-page">
    <el-row :gutter="20">
      <!-- 账户概览 -->
      <el-col :span="8">
        <el-card>
          <template #header>账户概览</template>
          <div class="account-overview">
            <div class="overview-item">
              <span class="label">总资产</span>
              <span class="value">${{ accountInfo.totalEquity?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="overview-item">
              <span class="label">持仓市值</span>
              <span class="value">${{ accountInfo.positionValue?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="overview-item">
              <span class="label">可用现金</span>
              <span class="value">${{ accountInfo.cashBalance?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="overview-item">
              <span class="label">今日盈亏</span>
              <span :class="['value', { profit: accountInfo.todayPnL > 0, loss: accountInfo.todayPnL < 0 }]">
                {{ accountInfo.todayPnL >= 0 ? '+' : '' }}${{ accountInfo.todayPnL?.toFixed(2) || '0.00' }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 持仓分布 -->
      <el-col :span="16">
        <el-card>
          <template #header>持仓分布</template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 持仓列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>持仓明细</span>
          <el-button type="primary" @click="refreshPositions">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="positions" v-loading="loading" stripe>
        <el-table-column prop="symbol" label="股票代码" width="120" />
        <el-table-column prop="name" label="股票名称" width="120" />
        <el-table-column prop="qty" label="持仓数量" width="100" />
        <el-table-column prop="availableQty" label="可用数量" width="100" />
        <el-table-column prop="costPrice" label="成本价" width="100">
          <template #default="{ row }">
            ${{ row.costPrice?.toFixed(4) || '0.0000' }}
          </template>
        </el-table-column>
        <el-table-column prop="mktPrice" label="现价" width="100">
          <template #default="{ row }">
            ${{ row.mktPrice?.toFixed(4) || '0.0000' }}
          </template>
        </el-table-column>
        <el-table-column prop="marketValue" label="市值" width="120">
          <template #default="{ row }">
            ${{ row.marketValue?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="pnl" label="盈亏" width="100">
          <template #default="{ row }">
            <span :class="{ profit: row.pnl > 0, loss: row.pnl < 0 }">
              {{ row.pnl >= 0 ? '+' : '' }}${{ row.pnl?.toFixed(2) || '0.00' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="pnlRatio" label="收益率" width="100">
          <template #default="{ row }">
            <span :class="{ profit: row.pnlRatio > 0, loss: row.pnlRatio < 0 }">
              {{ (row.pnlRatio >= 0 ? '+' : '') + (row.pnlRatio * 100)?.toFixed(2) || '0.00' }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="仓位占比" width="100">
          <template #default="{ row }">
            {{ (row.weight * 100)?.toFixed(2) || '0.00' }}%
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

interface Position {
  symbol: string
  name: string
  qty: number
  availableQty: number
  costPrice: number
  mktPrice: number
  marketValue: number
  pnl: number
  pnlRatio: number
  weight: number
}

interface AccountInfo {
  totalEquity: number
  positionValue: number
  cashBalance: number
  todayPnL: number
}

const loading = ref(false)
const positions = ref<Position[]>([])
const accountInfo = reactive<AccountInfo>({
  totalEquity: 0,
  positionValue: 0,
  cashBalance: 0,
  todayPnL: 0
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const fetchPositions = async () => {
  loading.value = true
  try {
    // TODO: 调用 API
    // const res = await positionApi.getPositions()
    // positions.value = res.data

    // 模拟数据
    positions.value = [
      { symbol: 'AAPL.US', name: '苹果', qty: 100, availableQty: 100, costPrice: 180.00, mktPrice: 185.50, marketValue: 18550, pnl: 550, pnlRatio: 0.0306, weight: 0.25 },
      { symbol: 'MSFT.US', name: '微软', qty: 50, availableQty: 50, costPrice: 380.00, mktPrice: 395.00, marketValue: 19750, pnl: 750, pnlRatio: 0.0196, weight: 0.27 },
      { symbol: 'GOOGL.US', name: '谷歌', qty: 30, availableQty: 30, costPrice: 140.00, mktPrice: 135.50, marketValue: 4065, pnl: -135, pnlRatio: -0.0321, weight: 0.05 },
      { symbol: 'TSLA.US', name: '特斯拉', qty: 80, availableQty: 80, costPrice: 200.00, mktPrice: 185.00, marketValue: 14800, pnl: -1200, pnlRatio: -0.075, weight: 0.20 },
      { symbol: 'NVDA.US', name: '英伟达', qty: 40, availableQty: 40, costPrice: 800.00, mktPrice: 890.00, marketValue: 35600, pnl: 3600, pnlRatio: 0.1125, weight: 0.23 }
    ]

    accountInfo.totalEquity = 92765
    accountInfo.positionValue = 92765
    accountInfo.cashBalance = 5000
    accountInfo.todayPnL = 3565

    updateChart()
  } catch (error) {
    ElMessage.error('获取持仓信息失败')
  } finally {
    loading.value = false
  }
}

const refreshPositions = () => {
  fetchPositions()
}

const initChart = () => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
}

const updateChart = () => {
  if (!chart) return

  const data = positions.value.map(p => ({
    name: p.symbol,
    value: p.marketValue
  }))

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: ${c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '持仓分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  })
}

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  fetchPositions()
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped lang="scss">
.position-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .account-overview {
    .overview-item {
      display: flex;
      justify-content: space-between;
      padding: 10px 0;
      border-bottom: 1px solid #ebeef5;

      &:last-child {
        border-bottom: none;
      }

      .label {
        color: #606266;
      }

      .value {
        font-weight: bold;
        color: #303133;

        &.profit {
          color: #67c23a;
        }

        &.loss {
          color: #f56c6c;
        }
      }
    }
  }

  .chart-container {
    height: 300px;
  }

  .profit {
    color: #67c23a;
  }

  .loss {
    color: #f56c6c;
  }
}
</style>

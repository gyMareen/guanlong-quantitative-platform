<template>
  <div class="risk-page">
    <el-row :gutter="20">
      <!-- 熔断状态 -->
      <el-col :span="8">
        <el-card>
          <template #header>熔断状态</template>
          <div class="circuit-breaker-status">
            <div class="status-indicator" :class="{ active: circuitBreaker.active }">
              <el-icon :size="48">
                <Warning v-if="circuitBreaker.active" />
                <CircleCheck v-else />
              </el-icon>
              <div class="status-text">
                {{ circuitBreaker.active ? '已熔断' : '正常运行' }}
              </div>
            </div>
            <div v-if="circuitBreaker.active" class="breaker-info">
              <p><strong>原因：</strong>{{ circuitBreaker.reason }}</p>
              <p><strong>触发时间：</strong>{{ circuitBreaker.triggeredAt }}</p>
            </div>
            <div class="action-buttons">
              <el-button
                v-if="!circuitBreaker.active"
                type="danger"
                @click="triggerBreaker"
              >
                手动熔断
              </el-button>
              <el-button
                v-else
                type="success"
                @click="resetBreaker"
              >
                解除熔断
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 今日风控统计 -->
      <el-col :span="16">
        <el-card>
          <template #header>今日风控统计</template>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ riskStats.totalOrders }}</div>
                <div class="stat-label">总订单数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value success">{{ riskStats.passedOrders }}</div>
                <div class="stat-label">通过订单</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value danger">{{ riskStats.rejectedOrders }}</div>
                <div class="stat-label">拒绝订单</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value warning">{{ riskStats.rejectRate }}%</div>
                <div class="stat-label">拒绝率</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 风控规则配置 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>风控规则配置</span>
          <el-button type="primary" @click="saveConfig">
            <el-icon><Check /></el-icon>
            保存配置
          </el-button>
        </div>
      </template>

      <el-form :model="riskConfig" label-width="160px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单票最大仓位">
              <el-input-number v-model="riskConfig.maxSinglePosition" :min="0" :max="1" :step="0.05" :precision="2" />
              <span class="input-suffix">（{{ (riskConfig.maxSinglePosition * 100).toFixed(0) }}%）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="组合最大仓位">
              <el-input-number v-model="riskConfig.maxTotalPosition" :min="0" :max="1" :step="0.05" :precision="2" />
              <span class="input-suffix">（{{ (riskConfig.maxTotalPosition * 100).toFixed(0) }}%）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单日最大亏损">
              <el-input-number v-model="riskConfig.maxDailyLoss" :min="0" :max="1" :step="0.01" :precision="2" />
              <span class="input-suffix">（{{ (riskConfig.maxDailyLoss * 100).toFixed(0) }}%）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单周最大亏损">
              <el-input-number v-model="riskConfig.maxWeeklyLoss" :min="0" :max="1" :step="0.01" :precision="2" />
              <span class="input-suffix">（{{ (riskConfig.maxWeeklyLoss * 100).toFixed(0) }}%）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大价格偏离">
              <el-input-number v-model="riskConfig.maxPriceDeviation" :min="0" :max="0.2" :step="0.01" :precision="2" />
              <span class="input-suffix">（{{ (riskConfig.maxPriceDeviation * 100).toFixed(0) }}%）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小交易金额">
              <el-input-number v-model="riskConfig.minTradeAmount" :min="1" :max="1000" :step="1" />
              <span class="input-suffix">（美元）</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 风控日志 -->
    <el-card style="margin-top: 20px">
      <template #header>风控日志</template>
      <el-table :data="riskLogs" stripe max-height="400">
        <el-table-column prop="timestamp" label="时间" width="160" />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="symbol" label="股票" width="120" />
        <el-table-column prop="rule" label="规则" width="150" />
        <el-table-column prop="message" label="详情" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const circuitBreaker = reactive({
  active: false,
  reason: '',
  triggeredAt: ''
})

const riskStats = reactive({
  totalOrders: 156,
  passedOrders: 142,
  rejectedOrders: 14,
  rejectRate: 9.0
})

const riskConfig = reactive({
  maxSinglePosition: 0.20,
  maxTotalPosition: 0.95,
  maxDailyLoss: 0.05,
  maxWeeklyLoss: 0.10,
  maxPriceDeviation: 0.04,
  minTradeAmount: 20
})

const riskLogs = ref([
  { timestamp: '2026-03-08 10:30:15', level: 'WARN', symbol: 'AAPL.US', rule: '仓位限制', message: '单票仓位超过20%限制' },
  { timestamp: '2026-03-08 10:25:30', level: 'INFO', symbol: 'TSLA.US', rule: '价格偏离', message: '价格偏离4.2%，超过4%阈值' },
  { timestamp: '2026-03-08 10:20:00', level: 'INFO', symbol: 'MSFT.US', rule: '最小金额', message: '交易金额$15.50，低于$20阈值' }
])

const fetchRiskConfig = async () => {
  // TODO: 调用 API
}

const saveConfig = async () => {
  try {
    // TODO: 调用 API
    ElMessage.success('配置保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

const triggerBreaker = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入熔断原因', '手动熔断', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入熔断原因'
    })
    // TODO: 调用 API
    circuitBreaker.active = true
    circuitBreaker.reason = value
    circuitBreaker.triggeredAt = new Date().toLocaleString()
    ElMessage.warning('熔断已触发')
  } catch {
    // 用户取消
  }
}

const resetBreaker = async () => {
  try {
    await ElMessageBox.confirm('确认解除熔断？', '提示', { type: 'warning' })
    // TODO: 调用 API
    circuitBreaker.active = false
    circuitBreaker.reason = ''
    circuitBreaker.triggeredAt = ''
    ElMessage.success('熔断已解除')
  } catch {
    // 用户取消
  }
}

const getLevelType = (level: string) => {
  const types: Record<string, string> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return types[level] || 'info'
}

onMounted(() => {
  fetchRiskConfig()
})
</script>

<style scoped lang="scss">
.risk-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .circuit-breaker-status {
    text-align: center;
    padding: 20px 0;

    .status-indicator {
      margin-bottom: 20px;

      .el-icon {
        color: #67c23a;
      }

      &.active .el-icon {
        color: #f56c6c;
      }

      .status-text {
        font-size: 18px;
        font-weight: bold;
        margin-top: 10px;
      }
    }

    .breaker-info {
      text-align: left;
      padding: 15px;
      background: #fef0f0;
      border-radius: 4px;
      margin-bottom: 20px;

      p {
        margin: 5px 0;
        color: #606266;
      }
    }

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 10px;
    }
  }

  .stat-item {
    text-align: center;
    padding: 20px;

    .stat-value {
      font-size: 32px;
      font-weight: bold;
      color: #303133;

      &.success {
        color: #67c23a;
      }

      &.danger {
        color: #f56c6c;
      }

      &.warning {
        color: #e6a23c;
      }
    }

    .stat-label {
      color: #909399;
      margin-top: 10px;
    }
  }

  .input-suffix {
    margin-left: 10px;
    color: #909399;
  }
}
</style>

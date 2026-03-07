-- 官龙量化一体化交易平台 - 数据库初始化脚本
-- Database: guanlong

-- =====================================================
-- 1. 采集原始数据表
-- =====================================================
CREATE TABLE IF NOT EXISTS rebalance_raw (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    allocation_cur DECIMAL(10,4),
    allocation_tar DECIMAL(10,4),
    symbol VARCHAR(20) NOT NULL,
    ref_price DECIMAL(10,4),
    payload_json JSONB,
    batch_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rebalance_raw_symbol ON rebalance_raw(symbol);
CREATE INDEX idx_rebalance_raw_ts ON rebalance_raw(ts);
CREATE INDEX idx_rebalance_raw_batch ON rebalance_raw(batch_id);

COMMENT ON TABLE rebalance_raw IS '富途调仓历史采集原始数据';
COMMENT ON COLUMN rebalance_raw.allocation_cur IS '当前权重';
COMMENT ON COLUMN rebalance_raw.allocation_tar IS '目标权重';

-- =====================================================
-- 2. 量化信号表
-- =====================================================
CREATE TABLE IF NOT EXISTS rebalance_qs (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    strategy VARCHAR(100) NOT NULL,
    strategy_version VARCHAR(50),
    symbol VARCHAR(20) NOT NULL,
    target_weight DECIMAL(10,4),
    target_position INTEGER,
    action VARCHAR(20) NOT NULL,
    score DECIMAL(10,4),
    price DECIMAL(10,4),
    note TEXT,
    params_hash VARCHAR(100),
    source VARCHAR(50) NOT NULL,
    payload_json JSONB,
    batch_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rebalance_qs_symbol ON rebalance_qs(symbol);
CREATE INDEX idx_rebalance_qs_strategy ON rebalance_qs(strategy);
CREATE INDEX idx_rebalance_qs_ts ON rebalance_qs(ts);
CREATE INDEX idx_rebalance_qs_batch ON rebalance_qs(batch_id);

COMMENT ON TABLE rebalance_qs IS '量化策略生成的交易信号';
COMMENT ON COLUMN rebalance_qs.action IS 'BUY/SELL/CLOSE/TARGET';
COMMENT ON COLUMN rebalance_qs.score IS '量化评分';

-- =====================================================
-- 3. 订单表
-- =====================================================
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50),
    signal_id INTEGER,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    order_type VARCHAR(20) DEFAULT 'LIMIT',
    price DECIMAL(10,4),
    qty INTEGER NOT NULL,
    filled_qty INTEGER DEFAULT 0,
    avg_price DECIMAL(10,4),
    status VARCHAR(20) DEFAULT 'PENDING',
    broker VARCHAR(50),
    broker_order_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    error_msg TEXT,
    metadata JSONB
);

CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at);
CREATE INDEX idx_orders_batch ON orders(batch_id);

COMMENT ON TABLE orders IS '交易订单记录';
COMMENT ON COLUMN orders.side IS 'BUY/SELL';
COMMENT ON COLUMN orders.status IS 'PENDING/SUBMITTED/FILLED/CANCELLED/REJECTED';

-- =====================================================
-- 4. 持仓快照表
-- =====================================================
CREATE TABLE IF NOT EXISTS positions_snapshot (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    symbol VARCHAR(20) NOT NULL,
    qty INTEGER NOT NULL,
    available_qty INTEGER,
    cost_price DECIMAL(10,4),
    mkt_price DECIMAL(10,4),
    market_value DECIMAL(20,4),
    pnl DECIMAL(20,4),
    pnl_ratio DECIMAL(10,4),
    market VARCHAR(20) DEFAULT 'US',
    account_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_positions_snapshot_symbol ON positions_snapshot(symbol);
CREATE INDEX idx_positions_snapshot_ts ON positions_snapshot(ts);
CREATE INDEX idx_positions_snapshot_account ON positions_snapshot(account_id);

COMMENT ON TABLE positions_snapshot IS '持仓快照记录';

-- =====================================================
-- 5. 执行日志表
-- =====================================================
CREATE TABLE IF NOT EXISTS exec_logs (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(10) NOT NULL,
    module VARCHAR(50),
    event VARCHAR(50),
    symbol VARCHAR(20),
    message TEXT NOT NULL,
    context_json JSONB,
    trace_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exec_logs_level ON exec_logs(level);
CREATE INDEX idx_exec_logs_module ON exec_logs(module);
CREATE INDEX idx_exec_logs_ts ON exec_logs(ts);
CREATE INDEX idx_exec_logs_symbol ON exec_logs(symbol);

COMMENT ON TABLE exec_logs IS '系统执行日志';

-- =====================================================
-- 6. 风控规则配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS risk_rules (
    id SERIAL PRIMARY KEY,
    rule_code VARCHAR(50) UNIQUE NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    description TEXT,
    params_json JSONB NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE risk_rules IS '风控规则配置';

-- 初始化风控规则
INSERT INTO risk_rules (rule_code, rule_name, description, params_json) VALUES
('position_limit', '仓位限制', '单票和组合仓位上限',
 '{"max_single_position": 0.20, "max_total_position": 0.95, "min_cash_reserve": 0.05}'),
('trade_limit', '交易限制', '最小交易金额和价格偏离保护',
 '{"min_trade_amount": 20, "max_price_deviation": 0.04, "max_daily_trades": 100}'),
('loss_limit', '亏损限制', '单日和单周亏损上限',
 '{"max_daily_loss": 0.05, "max_weekly_loss": 0.10, "max_consecutive_loss_days": 3}'),
('close_rule', '清仓规则', '目标为0时的清仓规则',
 '{"target_zero_action": "MARKET", "close_price_tolerance": 0.05}'),
('circuit_breaker', '熔断规则', '熔断器配置',
 '{"active": false, "triggered_at": null, "reason": null}')
ON CONFLICT (rule_code) DO NOTHING;

-- =====================================================
-- 7. 账户信息表
-- =====================================================
CREATE TABLE IF NOT EXISTS accounts (
    id SERIAL PRIMARY KEY,
    account_id VARCHAR(50) UNIQUE NOT NULL,
    broker VARCHAR(50) NOT NULL,
    account_type VARCHAR(20),
    currency VARCHAR(10) DEFAULT 'USD',
    total_cash DECIMAL(20,4),
    total_equity DECIMAL(20,4),
    available_cash DECIMAL(20,4),
    margin_used DECIMAL(20,4),
    updated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE accounts IS '账户信息';

-- =====================================================
-- 8. 系统配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS system_config (
    id SERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE system_config IS '系统配置';

-- 初始化系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('trade.min_amount', '20', '最小交易金额（美元）'),
('trade.price_deviation', '0.04', '最大价格偏离'),
('kafka.topic.raw', 'futu.rebalance.raw', '采集信号Topic'),
('kafka.topic.qs', 'futu.rebalance.qs', '量化信号Topic'),
('kafka.topic.events', 'futu.trade.events', '交易事件Topic'),
('broker.default', 'longport', '默认券商')
ON CONFLICT (config_key) DO NOTHING;

-- =====================================================
-- 9. 用户表（复用已有结构）
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_user_username ON sys_user(username);

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO sys_user (username, password, email, nickname, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@guanlong.com', '管理员', 1)
ON CONFLICT (username) DO NOTHING;

-- =====================================================
-- 10. 视图：活跃订单
-- =====================================================
CREATE OR REPLACE VIEW v_active_orders AS
SELECT
    o.*,
    p.qty as position_qty,
    p.mkt_price as current_price
FROM orders o
LEFT JOIN positions_snapshot p ON o.symbol = p.symbol
WHERE o.status IN ('PENDING', 'SUBMITTED', 'PARTIAL_FILLED')
ORDER BY o.created_at DESC;

-- =====================================================
-- 11. 视图：今日交易统计
-- =====================================================
CREATE OR REPLACE VIEW v_today_stats AS
SELECT
    COUNT(*) as order_count,
    SUM(CASE WHEN side = 'BUY' THEN 1 ELSE 0 END) as buy_count,
    SUM(CASE WHEN side = 'SELL' THEN 1 ELSE 0 END) as sell_count,
    SUM(CASE WHEN status = 'FILLED' THEN qty * avg_price ELSE 0 END) as filled_amount,
    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count
FROM orders
WHERE created_at >= CURRENT_DATE;

-- =====================================================
-- 完成
-- =====================================================
SELECT 'Database initialization completed!' as status;

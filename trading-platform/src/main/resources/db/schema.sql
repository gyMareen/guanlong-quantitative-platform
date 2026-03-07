-- 官龙量化交易平台数据库架构
-- PostgreSQL 14+

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- 初始化管理员账户 (密码: admin123)
INSERT INTO users (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@guanlong.com', 1)
ON CONFLICT (username) DO NOTHING;

-- 信号表
CREATE TABLE IF NOT EXISTS signals (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    target_weight DECIMAL(10, 4),
    target_position INTEGER,
    score DECIMAL(10, 4),
    strategy VARCHAR(100),
    strategy_version VARCHAR(50),
    params_hash VARCHAR(64),
    source VARCHAR(50),
    note TEXT,
    batch_id VARCHAR(64),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_signals_symbol ON signals(symbol);
CREATE INDEX idx_signals_strategy ON signals(strategy);
CREATE INDEX idx_signals_timestamp ON signals(timestamp);
CREATE INDEX idx_signals_batch_id ON signals(batch_id);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(64),
    signal_id BIGINT REFERENCES signals(id),
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    price DECIMAL(20, 6),
    qty INTEGER NOT NULL,
    filled_qty INTEGER DEFAULT 0,
    avg_price DECIMAL(20, 6),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    broker VARCHAR(50),
    broker_order_id VARCHAR(100),
    error_msg TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_batch_id ON orders(batch_id);
CREATE INDEX idx_orders_broker_order_id ON orders(broker_order_id);

-- 风控规则表
CREATE TABLE IF NOT EXISTS risk_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    description TEXT,
    params_json TEXT,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化风控规则数据
INSERT INTO risk_rules (rule_code, rule_name, description, params_json, enabled)
VALUES
    ('position_limit', '单票仓位限制', '限制单只股票的最大仓位比例', '{"maxRatio": 0.20}', true),
    ('trade_limit', '交易金额限制', '限制单笔交易的最小金额', '{"minAmount": 20}', true),
    ('loss_limit', '亏损限制', '限制日内和周内最大亏损比例', '{"dailyLossLimit": 0.05, "weeklyLossLimit": 0.10}', true),
    ('close_rule', '收盘规则', '控制收盘前的交易行为', '{"closeTime": "15:00", "allowCloseTrade": true}', true)
ON CONFLICT (rule_code) DO NOTHING;

-- 持仓快照表（用于历史记录）
CREATE TABLE IF NOT EXISTS position_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    qty INTEGER NOT NULL,
    cost_price DECIMAL(20, 6),
    market_price DECIMAL(20, 6),
    market_value DECIMAL(20, 6),
    pnl DECIMAL(20, 6),
    pnl_ratio DECIMAL(10, 4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_position_snapshots_date ON position_snapshots(snapshot_date);
CREATE INDEX idx_position_snapshots_symbol ON position_snapshots(symbol);

-- 交易日志表
CREATE TABLE IF NOT EXISTS trading_logs (
    id BIGSERIAL PRIMARY KEY,
    log_type VARCHAR(20) NOT NULL,
    reference_id VARCHAR(100),
    symbol VARCHAR(20),
    message TEXT,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trading_logs_type ON trading_logs(log_type);
CREATE INDEX idx_trading_logs_reference ON trading_logs(reference_id);
CREATE INDEX idx_trading_logs_created_at ON trading_logs(created_at);

-- 策略配置表
CREATE TABLE IF NOT EXISTS strategy_configs (
    id BIGSERIAL PRIMARY KEY,
    strategy_code VARCHAR(50) NOT NULL UNIQUE,
    strategy_name VARCHAR(100) NOT NULL,
    description TEXT,
    config_json TEXT,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户配置表
CREATE TABLE IF NOT EXISTS user_configs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, config_key)
);

CREATE INDEX idx_user_configs_user ON user_configs(user_id);

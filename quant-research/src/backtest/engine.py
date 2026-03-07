"""回测引擎"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional

import numpy as np
import pandas as pd

from config import settings
from ..strategies.base import BaseStrategy

logger = logging.getLogger(__name__)


@dataclass
class BacktestConfig:
    """回测配置"""
    initial_cash: float = 1_000_000.0
    commission: float = 0.001  # 0.1%
    slippage: float = 0.0005  # 0.05%
    position_size: float = 0.95  # 仓位比例


@dataclass
class BacktestResult:
    """回测结果"""
    strategy_name: str
    start_date: datetime
    end_date: datetime
    initial_cash: float
    final_value: float
    total_return: float
    annual_return: float
    max_drawdown: float
    sharpe_ratio: float
    win_rate: float
    trades: List[Dict] = field(default_factory=list)
    equity_curve: pd.DataFrame = field(default_factory=pd.DataFrame)

    def summary(self) -> str:
        return f"""
回测结果 - {self.strategy_name}
{'='*40}
回测区间: {self.start_date.date()} ~ {self.end_date.date()}
初始资金: ${self.initial_cash:,.2f}
最终价值: ${self.final_value:,.2f}
总收益率: {self.total_return:.2%}
年化收益: {self.annual_return:.2%}
最大回撤: {self.max_drawdown:.2%}
夏普比率: {self.sharpe_ratio:.2f}
胜率: {self.win_rate:.2%}
交易次数: {len(self.trades)}
"""


class BacktestEngine:
    """回测引擎"""

    def __init__(self, config: Optional[BacktestConfig] = None):
        self.config = config or BacktestConfig()

    def run(
        self,
        strategy: BaseStrategy,
        df: pd.DataFrame,
        symbol: str = "ASSET"
    ) -> BacktestResult:
        """运行回测

        Args:
            strategy: 策略实例
            df: OHLCV 数据
            symbol: 资产代码

        Returns:
            回测结果
        """
        # 生成信号
        signals = strategy.generate_signals(df)
        strategy.set_data(df)

        # 计算目标权重
        weights = strategy.compute_weights(signals)

        # 初始化
        cash = self.config.initial_cash
        position = 0.0
        equity_curve = []
        trades = []

        # 遍历每一天
        for i, (date, row) in enumerate(df.iterrows()):
            if i == 0 or pd.isna(weights.iloc[i]):
                equity_curve.append({
                    "date": date,
                    "cash": cash,
                    "position": position,
                    "price": row["close"],
                    "equity": cash + position * row["close"]
                })
                continue

            target_weight = weights.iloc[i]
            current_equity = cash + position * row["close"]
            target_position_value = current_equity * target_weight * self.config.position_size

            # 计算需要调整的仓位
            current_position_value = position * row["close"]
            trade_value = target_position_value - current_position_value

            # 执行交易
            if abs(trade_value) > 100:  # 最小交易金额
                # 考虑滑点
                execution_price = row["close"] * (1 + np.sign(trade_value) * self.config.slippage)

                # 计算交易数量
                trade_qty = trade_value / execution_price

                # 更新持仓和现金
                new_position = position + trade_qty
                trade_cost = abs(trade_qty * execution_price)
                commission = trade_cost * self.config.commission

                cash = cash - trade_value - commission
                position = new_position

                trades.append({
                    "date": date,
                    "symbol": symbol,
                    "side": "BUY" if trade_qty > 0 else "SELL",
                    "price": execution_price,
                    "qty": abs(trade_qty),
                    "value": trade_cost,
                    "commission": commission,
                })

            # 记录权益
            equity_curve.append({
                "date": date,
                "cash": cash,
                "position": position,
                "price": row["close"],
                "equity": cash + position * row["close"]
            })

        # 构建结果
        equity_df = pd.DataFrame(equity_curve)
        equity_df = equity_df.set_index("date")

        final_equity = cash + position * df["close"].iloc[-1]
        total_return = (final_equity - self.config.initial_cash) / self.config.initial_cash

        # 计算年化收益
        days = (df.index[-1] - df.index[0]).days
        annual_return = (1 + total_return) ** (365 / days) - 1 if days > 0 else 0

        # 计算最大回撤
        equity_series = equity_df["equity"]
        rolling_max = equity_series.cummax()
        drawdown = (equity_series - rolling_max) / rolling_max
        max_drawdown = drawdown.min()

        # 计算夏普比率
        returns = equity_series.pct_change().dropna()
        sharpe_ratio = (returns.mean() * 252) / (returns.std() * np.sqrt(252)) if returns.std() > 0 else 0

        # 计算胜率
        winning_trades = [t for t in trades if t["side"] == "SELL"]
        win_rate = len(winning_trades) / len(trades) if trades else 0

        return BacktestResult(
            strategy_name=strategy.name,
            start_date=df.index[0],
            end_date=df.index[-1],
            initial_cash=self.config.initial_cash,
            final_value=final_equity,
            total_return=total_return,
            annual_return=annual_return,
            max_drawdown=max_drawdown,
            sharpe_ratio=sharpe_ratio,
            win_rate=win_rate,
            trades=trades,
            equity_curve=equity_df,
        )


def run_backtest(
    strategy: BaseStrategy,
    df: pd.DataFrame,
    **kwargs
) -> BacktestResult:
    """便捷函数：运行回测"""
    engine = BacktestEngine(**kwargs)
    return engine.run(strategy, df)

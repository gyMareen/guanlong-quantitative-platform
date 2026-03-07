"""趋势类策略"""

import numpy as np
import pandas as pd

from .base import BaseStrategy, Signal, StrategyConfig


class MATrendStrategy(BaseStrategy):
    """移动平均趋势策略

    当短期均线上穿长期均线时买入，下穿时卖出
    """

    def __init__(
        self,
        fast_period: int = 10,
        slow_period: int = 30,
        config: StrategyConfig = None
    ):
        if config is None:
            config = StrategyConfig(
                name="ma_trend",
                params={"fast_period": fast_period, "slow_period": slow_period}
            )
        super().__init__(config)
        self.fast_period = fast_period
        self.slow_period = slow_period

    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]

        # 计算移动平均
        fast_ma = close.rolling(window=self.fast_period).mean()
        slow_ma = close.rolling(window=self.slow_period).mean()

        # 计算信号
        signals = pd.Series(0, index=df.index)

        # 金叉买入
        golden_cross = (fast_ma > slow_ma) & (fast_ma.shift(1) <= slow_ma.shift(1))
        signals[golden_cross] = Signal.BUY.value

        # 死叉卖出
        death_cross = (fast_ma < slow_ma) & (fast_ma.shift(1) >= slow_ma.shift(1))
        signals[death_cross] = Signal.SELL.value

        return signals

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算评分：基于均线距离"""
        close = df["close"]

        fast_ma = close.rolling(window=self.fast_period).mean()
        slow_ma = close.rolling(window=self.slow_period).mean()

        # 均线距离百分比
        ma_distance = (fast_ma - slow_ma) / slow_ma

        # 标准化为 [-1, 1]
        scores = np.tanh(ma_distance * 10)
        return scores


class BreakoutStrategy(BaseStrategy):
    """突破策略

    价格突破 N 日最高价买入，跌破 N 日最低价卖出
    """

    def __init__(
        self,
        period: int = 20,
        config: StrategyConfig = None
    ):
        if config is None:
            config = StrategyConfig(
                name="breakout",
                params={"period": period}
            )
        super().__init__(config)
        self.period = period

    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        high = df["high"]
        low = df["low"]
        close = df["close"]

        # 计算通道
        upper_band = high.rolling(window=self.period).max().shift(1)
        lower_band = low.rolling(window=self.period).min().shift(1)

        # 计算信号
        signals = pd.Series(0, index=df.index)

        # 突破上轨买入
        breakout_up = close > upper_band
        signals[breakout_up] = Signal.BUY.value

        # 跌破下轨卖出
        breakout_down = close < lower_band
        signals[breakout_down] = Signal.SELL.value

        return signals

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算评分：基于价格在通道中的位置"""
        high = df["high"]
        low = df["low"]
        close = df["close"]

        upper_band = high.rolling(window=self.period).max()
        lower_band = low.rolling(window=self.period).min()

        # 价格在通道中的位置 (0-1)
        position = (close - lower_band) / (upper_band - lower_band)

        # 标准化为 [-1, 1]，中点为 0
        scores = (position - 0.5) * 2
        return scores

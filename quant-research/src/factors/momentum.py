"""动量因子"""

import numpy as np
import pandas as pd

from .base import Factor


class MomentumFactor(Factor):
    """动量因子

    计算过去 N 天的收益率
    """

    def __init__(self, period: int = 20, name: str = "momentum"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        returns = df["close"].pct_change(self.period)
        return returns


class ROCPFactor(Factor):
    """Rate of Change Percentage (ROCP)

    (close - close_n) / close_n
    """

    def __init__(self, period: int = 12, name: str = "rocp"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]
        rocp = (close - close.shift(self.period)) / close.shift(self.period)
        return rocp


class RSI(Factor):
    """Relative Strength Index (RSI)

    RSI = 100 - 100 / (1 + RS)
    RS = Average Gain / Average Loss
    """

    def __init__(self, period: int = 14, name: str = "rsi"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]
        delta = close.diff()

        gain = delta.where(delta > 0, 0)
        loss = (-delta).where(delta < 0, 0)

        avg_gain = gain.rolling(window=self.period, min_periods=self.period).mean()
        avg_loss = loss.rolling(window=self.period, min_periods=self.period).mean()

        rs = avg_gain / avg_loss
        rsi = 100 - (100 / (1 + rs))

        return rsi


class MACD(Factor):
    """Moving Average Convergence Divergence (MACD)"""

    def __init__(
        self,
        fast_period: int = 12,
        slow_period: int = 26,
        signal_period: int = 9,
        name: str = "macd"
    ):
        super().__init__(name)
        self.fast_period = fast_period
        self.slow_period = slow_period
        self.signal_period = signal_period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]

        ema_fast = close.ewm(span=self.fast_period, adjust=False).mean()
        ema_slow = close.ewm(span=self.slow_period, adjust=False).mean()

        macd_line = ema_fast - ema_slow
        return macd_line

    def compute_histogram(self, df: pd.DataFrame) -> pd.Series:
        """计算 MACD 柱状图"""
        macd_line = self.compute(df)
        signal_line = macd_line.ewm(span=self.signal_period, adjust=False).mean()
        histogram = macd_line - signal_line
        return histogram

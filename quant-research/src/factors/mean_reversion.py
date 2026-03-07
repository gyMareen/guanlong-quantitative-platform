"""均值回归因子"""

import numpy as np
import pandas as pd
from scipy import stats

from .base import Factor


class ZScoreFactor(Factor):
    """Z-Score 因子

    价格相对于均值的标准化偏离程度
    """

    def __init__(self, period: int = 20, name: str = "zscore"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]

        mean = close.rolling(window=self.period).mean()
        std = close.rolling(window=self.period).std()

        zscore = (close - mean) / std
        return zscore


class MeanReversionFactor(Factor):
    """均值回归因子

    基于价格偏离均值的程度，用于判断超买/超卖
    """

    def __init__(self, period: int = 20, name: str = "mean_reversion"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]

        # 计算价格相对于均值的偏离
        mean = close.rolling(window=self.period).mean()
        deviation = (close - mean) / mean

        # 负值表示价格低于均值（可能超卖），正值表示高于均值（可能超买）
        return -deviation  # 取负值，使得超卖为正信号


class OUProcessFactor(Factor):
    """Ornstein-Uhlenbeck 过程因子

    基于均值回复速度和半衰期
    """

    def __init__(self, period: int = 60, name: str = "ou_process"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]
        returns = close.pct_change().dropna()

        def calc_ou_score(window_returns):
            if len(window_returns) < 10:
                return np.nan

            # 简化的 OU 参数估计
            try:
                mean_reversion = -np.corrcoef(window_returns[:-1], window_returns[1:])[0, 1]
                return mean_reversion
            except:
                return np.nan

        ou_score = returns.rolling(window=self.period).apply(calc_ou_score, raw=False)
        return ou_score


class RSVFactor(Factor):
    """Raw Stochastic Value (RSV)

    用于计算 KDJ 指标
    """

    def __init__(self, period: int = 9, name: str = "rsv"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]
        high = df["high"]
        low = df["low"]

        lowest_low = low.rolling(window=self.period).min()
        highest_high = high.rolling(window=self.period).max()

        rsv = (close - lowest_low) / (highest_high - lowest_low) * 100
        return rsv


class WilliamsRFactor(Factor):
    """Williams %R 指标

    衡量超买超卖水平
    """

    def __init__(self, period: int = 14, name: str = "williams_r"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]
        high = df["high"]
        low = df["low"]

        highest_high = high.rolling(window=self.period).max()
        lowest_low = low.rolling(window=self.period).min()

        williams_r = (highest_high - close) / (highest_high - lowest_low) * -100
        return williams_r

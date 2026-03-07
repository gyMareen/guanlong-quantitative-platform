"""波动率因子"""

import numpy as np
import pandas as pd

from .base import Factor


class VolatilityFactor(Factor):
    """历史波动率因子

    计算过去 N 天收益率的标准差（年化）
    """

    def __init__(self, period: int = 20, annualize: bool = True, name: str = "volatility"):
        super().__init__(name)
        self.period = period
        self.annualize = annualize

    def compute(self, df: pd.DataFrame) -> pd.Series:
        returns = df["close"].pct_change()
        volatility = returns.rolling(window=self.period).std()

        if self.annualize:
            volatility = volatility * np.sqrt(252)

        return volatility


class ATR(Factor):
    """Average True Range (ATR)

    衡量市场波动性
    """

    def __init__(self, period: int = 14, name: str = "atr"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        high = df["high"]
        low = df["low"]
        close = df["close"]

        tr1 = high - low
        tr2 = abs(high - close.shift(1))
        tr3 = abs(low - close.shift(1))

        true_range = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        atr = true_range.rolling(window=self.period).mean()

        return atr


class BollingerBandsWidth(Factor):
    """布林带宽度因子

    衡量价格相对于布林带的位置
    """

    def __init__(self, period: int = 20, num_std: float = 2.0, name: str = "bb_width"):
        super().__init__(name)
        self.period = period
        self.num_std = num_std

    def compute(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]

        middle_band = close.rolling(window=self.period).mean()
        std = close.rolling(window=self.period).std()

        upper_band = middle_band + (std * self.num_std)
        lower_band = middle_band - (std * self.num_std)

        bb_width = (upper_band - lower_band) / middle_band
        return bb_width

    def compute_position(self, df: pd.DataFrame) -> pd.Series:
        """计算价格在布林带中的位置 (0-1)"""
        close = df["close"]

        middle_band = close.rolling(window=self.period).mean()
        std = close.rolling(window=self.period).std()

        upper_band = middle_band + (std * self.num_std)
        lower_band = middle_band - (std * self.num_std)

        position = (close - lower_band) / (upper_band - lower_band)
        return position


class ParkinsonVolatility(Factor):
    """Parkinson 波动率

    使用高低价计算波动率，对日内波动更敏感
    """

    def __init__(self, period: int = 20, name: str = "parkinson_vol"):
        super().__init__(name)
        self.period = period

    def compute(self, df: pd.DataFrame) -> pd.Series:
        high = df["high"]
        low = df["low"]

        hl_ratio = np.log(high / low)
        hl_squared = hl_ratio ** 2

        # Parkinson 波动率常数
        k = 1 / (4 * np.log(2))

        volatility = np.sqrt(k * hl_squared.rolling(window=self.period).mean())
        volatility = volatility * np.sqrt(252)  # 年化

        return volatility

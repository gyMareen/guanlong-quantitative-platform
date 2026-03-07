"""均值回归策略"""

import numpy as np
import pandas as pd

from .base import BaseStrategy, Signal, StrategyConfig


class BollingerBandsStrategy(BaseStrategy):
    """布林带策略

    价格触及下轨买入，触及上轨卖出
    """

    def __init__(
        self,
        period: int = 20,
        num_std: float = 2.0,
        config: StrategyConfig = None
    ):
        if config is None:
            config = StrategyConfig(
                name="bollinger_bands",
                params={"period": period, "num_std": num_std}
            )
        super().__init__(config)
        self.period = period
        self.num_std = num_std

    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]

        # 计算布林带
        middle_band = close.rolling(window=self.period).mean()
        std = close.rolling(window=self.period).std()

        upper_band = middle_band + (std * self.num_std)
        lower_band = middle_band - (std * self.num_std)

        # 计算信号
        signals = pd.Series(0, index=df.index)

        # 触及下轨买入
        touch_lower = close <= lower_band
        signals[touch_lower] = Signal.BUY.value

        # 触及上轨卖出
        touch_upper = close >= upper_band
        signals[touch_upper] = Signal.SELL.value

        return signals

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算评分：基于 %B 指标"""
        close = df["close"]

        middle_band = close.rolling(window=self.period).mean()
        std = close.rolling(window=self.period).std()

        upper_band = middle_band + (std * self.num_std)
        lower_band = middle_band - (std * self.num_std)

        # %B 指标 (0-1)
        percent_b = (close - lower_band) / (upper_band - lower_band)

        # 标准化为 [-1, 1]，低于 0.2 超卖，高于 0.8 超买
        scores = np.where(percent_b < 0.2, 1 - percent_b / 0.2,
                         np.where(percent_b > 0.8, -(percent_b - 0.8) / 0.2, 0))
        return pd.Series(scores, index=df.index)


class RSIStrategy(BaseStrategy):
    """RSI 策略

    RSI 低于超卖线买入，高于超买线卖出
    """

    def __init__(
        self,
        period: int = 14,
        oversold: float = 30,
        overbought: float = 70,
        config: StrategyConfig = None
    ):
        if config is None:
            config = StrategyConfig(
                name="rsi",
                params={"period": period, "oversold": oversold, "overbought": overbought}
            )
        super().__init__(config)
        self.period = period
        self.oversold = oversold
        self.overbought = overbought

    def compute_rsi(self, close: pd.Series) -> pd.Series:
        """计算 RSI"""
        delta = close.diff()

        gain = delta.where(delta > 0, 0)
        loss = (-delta).where(delta < 0, 0)

        avg_gain = gain.rolling(window=self.period, min_periods=self.period).mean()
        avg_loss = loss.rolling(window=self.period, min_periods=self.period).mean()

        rs = avg_gain / avg_loss
        rsi = 100 - (100 / (1 + rs))

        return rsi

    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        close = df["close"]
        rsi = self.compute_rsi(close)

        # 计算信号
        signals = pd.Series(0, index=df.index)

        # RSI 低于超卖线买入
        oversold_signal = rsi < self.oversold
        signals[oversold_signal] = Signal.BUY.value

        # RSI 高于超买线卖出
        overbought_signal = rsi > self.overbought
        signals[overbought_signal] = Signal.SELL.value

        return signals

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算评分：基于 RSI 偏离"""
        close = df["close"]
        rsi = self.compute_rsi(close)

        # 标准化为 [-1, 1]
        scores = (50 - rsi) / 50
        return scores


class MeanReversionComboStrategy(BaseStrategy):
    """均值回归组合策略

    结合多个均值回归指标
    """

    def __init__(
        self,
        rsi_period: int = 14,
        bb_period: int = 20,
        zscore_period: int = 20,
        config: StrategyConfig = None
    ):
        if config is None:
            config = StrategyConfig(
                name="mean_reversion_combo",
                params={
                    "rsi_period": rsi_period,
                    "bb_period": bb_period,
                    "zscore_period": zscore_period
                }
            )
        super().__init__(config)
        self.rsi_period = rsi_period
        self.bb_period = bb_period
        self.zscore_period = zscore_period

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算综合评分"""
        close = df["close"]

        # RSI 评分
        rsi_strategy = RSIStrategy(self.rsi_period)
        rsi_scores = rsi_strategy.compute_scores(df)

        # 布林带评分
        bb_strategy = BollingerBandsStrategy(self.bb_period)
        bb_scores = bb_strategy.compute_scores(df)

        # Z-Score 评分
        mean = close.rolling(window=self.zscore_period).mean()
        std = close.rolling(window=self.zscore_period).std()
        zscore = (close - mean) / std
        zscore_scores = -zscore / 3  # 取负值，负 Z-Score 表示超卖

        # 综合评分（等权平均）
        combined_scores = (rsi_scores + bb_scores + zscore_scores) / 3

        # 限制在 [-1, 1]
        combined_scores = combined_scores.clip(-1, 1)

        return combined_scores

    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        scores = self.compute_scores(df)

        signals = pd.Series(0, index=df.index)
        signals[scores > 0.5] = Signal.BUY.value
        signals[scores < -0.5] = Signal.SELL.value

        return signals

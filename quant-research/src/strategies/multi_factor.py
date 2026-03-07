"""多因子策略"""

import numpy as np
import pandas as pd

from ..factors import (
    ATR,
    BollingerBandsWidth,
    MACD,
    MomentumFactor,
    RSI,
    VolatilityFactor,
    ZScoreFactor,
)
from .base import BaseStrategy, Signal, StrategyConfig


class MultiFactorStrategy(BaseStrategy):
    """多因子评分策略

    结合多个因子生成综合评分
    """

    def __init__(
        self,
        momentum_period: int = 20,
        volatility_period: int = 20,
        rsi_period: int = 14,
        macd_fast: int = 12,
        macd_slow: int = 26,
        bb_period: int = 20,
        zscore_period: int = 20,
        config: StrategyConfig = None
    ):
        if config is None:
            config = StrategyConfig(
                name="multi_factor",
                version="1.0.0",
                params={
                    "momentum_period": momentum_period,
                    "volatility_period": volatility_period,
                    "rsi_period": rsi_period,
                    "macd_fast": macd_fast,
                    "macd_slow": macd_slow,
                    "bb_period": bb_period,
                    "zscore_period": zscore_period,
                }
            )
        super().__init__(config)

        # 初始化因子
        self.factors = {
            "momentum": MomentumFactor(momentum_period),
            "volatility": VolatilityFactor(volatility_period),
            "rsi": RSI(rsi_period),
            "macd": MACD(macd_fast, macd_slow),
            "bb_width": BollingerBandsWidth(bb_period),
            "zscore": ZScoreFactor(zscore_period),
        }

        # 因子权重
        self.weights = {
            "momentum": 0.25,
            "rsi": 0.20,
            "macd": 0.20,
            "bb_width": 0.15,
            "zscore": 0.20,
        }

    def compute_factor_scores(self, df: pd.DataFrame) -> pd.DataFrame:
        """计算各因子评分"""
        scores = pd.DataFrame(index=df.index)

        # 动量因子（标准化）
        momentum = self.factors["momentum"].compute(df)
        scores["momentum"] = np.tanh(momentum * 10)

        # RSI 因子（反转）
        rsi = self.factors["rsi"].compute(df)
        scores["rsi"] = (50 - rsi) / 50

        # MACD 因子
        macd = self.factors["macd"].compute(df)
        macd_normalized = macd / df["close"]
        scores["macd"] = np.tanh(macd_normalized * 100)

        # 布林带宽度（低宽度可能预示突破）
        bb_width = self.factors["bb_width"].compute(df)
        scores["bb_width"] = -bb_width  # 低宽度为正信号

        # Z-Score 因子（均值回归）
        zscore = self.factors["zscore"].compute(df)
        scores["zscore"] = -zscore / 3

        return scores

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算综合评分"""
        factor_scores = self.compute_factor_scores(df)

        # 加权平均
        combined_score = pd.Series(0.0, index=df.index)
        total_weight = 0.0

        for factor_name, weight in self.weights.items():
            if factor_name in factor_scores.columns:
                combined_score += factor_scores[factor_name].fillna(0) * weight
                total_weight += weight

        if total_weight > 0:
            combined_score = combined_score / total_weight

        # 限制在 [-1, 1]
        combined_score = combined_score.clip(-1, 1)

        return combined_score

    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        scores = self.compute_scores(df)

        signals = pd.Series(0, index=df.index)

        # 强买入信号
        signals[scores > 0.6] = Signal.BUY.value

        # 强卖出信号
        signals[scores < -0.6] = Signal.SELL.value

        return signals

    def compute_weights(self, signals: pd.Series) -> pd.Series:
        """根据评分计算目标权重"""
        scores = self.compute_scores(self._data) if self._data is not None else signals

        # 评分转换为权重
        # 评分 > 0.6: 权重 0.8-1.0
        # 评分 0.3-0.6: 权重 0.5-0.8
        # 评分 0-0.3: 权重 0.3-0.5
        # 评分 < 0: 权重 0

        weights = pd.Series(0.0, index=scores.index)

        weights[scores > 0.6] = 0.8 + (scores[scores > 0.6] - 0.6) * 0.5
        weights[(scores > 0.3) & (scores <= 0.6)] = 0.5 + (scores[(scores > 0.3) & (scores <= 0.6)] - 0.3) * 1.0
        weights[(scores > 0) & (scores <= 0.3)] = 0.3 + scores[(scores > 0) & (scores <= 0.3)] * 0.7

        return weights.clip(0, 1)

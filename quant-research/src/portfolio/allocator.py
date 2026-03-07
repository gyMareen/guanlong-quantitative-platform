"""组合管理模块"""

from typing import Dict, List, Optional

import numpy as np
import pandas as pd

from config import settings
from ..signals.models import SignalModel


class PortfolioAllocator:
    """组合分配器

    负责将多个信号融合为最终的目标权重
    """

    def __init__(
        self,
        max_single_position: float = None,
        max_total_position: float = None,
        min_cash_reserve: float = 0.05
    ):
        self.max_single_position = max_single_position or settings.MAX_SINGLE_POSITION
        self.max_total_position = max_total_position or settings.MAX_TOTAL_POSITION
        self.min_cash_reserve = min_cash_reserve

    def fuse_signals(
        self,
        signals: List[SignalModel],
        method: str = "weighted_average"
    ) -> Dict[str, float]:
        """融合多个信号

        Args:
            signals: 信号列表
            method: 融合方法 (weighted_average, max_score, consensus)

        Returns:
            目标权重字典 {symbol: weight}
        """
        # 按 symbol 分组
        symbol_signals: Dict[str, List[SignalModel]] = {}
        for signal in signals:
            if signal.symbol not in symbol_signals:
                symbol_signals[signal.symbol] = []
            symbol_signals[signal.symbol].append(signal)

        # 融合每个 symbol 的信号
        fused_weights = {}
        for symbol, sigs in symbol_signals.items():
            if method == "weighted_average":
                weight = self._weighted_average(sigs)
            elif method == "max_score":
                weight = self._max_score(sigs)
            elif method == "consensus":
                weight = self._consensus(sigs)
            else:
                raise ValueError(f"Unknown fusion method: {method}")

            fused_weights[symbol] = weight

        return fused_weights

    def _weighted_average(self, signals: List[SignalModel]) -> float:
        """加权平均"""
        total_weight = 0.0
        total_score = 0.0

        for signal in signals:
            weight = abs(signal.score) if signal.score else 0.5
            target = signal.target_weight if signal.target_weight else 0

            total_score += target * weight
            total_weight += weight

        return total_score / total_weight if total_weight > 0 else 0

    def _max_score(self, signals: List[SignalModel]) -> float:
        """取评分最高的信号"""
        best_signal = max(signals, key=lambda s: abs(s.score or 0))
        return best_signal.target_weight or 0

    def _consensus(self, signals: List[SignalModel]) -> float:
        """共识机制：多数同意才采用"""
        buy_count = sum(1 for s in signals if s.action == "BUY")
        sell_count = sum(1 for s in signals if s.action == "SELL")

        if buy_count > sell_count:
            return self._weighted_average([s for s in signals if s.action == "BUY"])
        elif sell_count > buy_count:
            return self._weighted_average([s for s in signals if s.action == "SELL"])
        else:
            return 0

    def normalize_weights(
        self,
        weights: Dict[str, float],
        method: str = "simple"
    ) -> Dict[str, float]:
        """权重归一化

        Args:
            weights: 原始权重
            method: 归一化方法 (simple, risk_parity)

        Returns:
            归一化后的权重
        """
        if not weights:
            return {}

        # 应用单票上限
        for symbol in weights:
            weights[symbol] = min(weights[symbol], self.max_single_position)

        # 计算总权重
        total_weight = sum(weights.values())

        # 如果超过总仓位上限，按比例缩减
        max_weight = self.max_total_position - self.min_cash_reserve
        if total_weight > max_weight:
            scale = max_weight / total_weight
            weights = {s: w * scale for s, w in weights.items()}

        return weights

    def apply_risk_constraints(
        self,
        weights: Dict[str, float],
        current_positions: Optional[Dict[str, float]] = None
    ) -> Dict[str, float]:
        """应用风险约束

        Args:
            weights: 目标权重
            current_positions: 当前持仓权重

        Returns:
            调整后的目标权重
        """
        # 归一化
        weights = self.normalize_weights(weights)

        # 如果有当前持仓，可以考虑换手率限制
        # 这里暂时不做额外限制

        return weights


def allocate_portfolio(
    signals: List[SignalModel],
    current_positions: Optional[Dict[str, float]] = None,
    fusion_method: str = "weighted_average"
) -> Dict[str, float]:
    """便捷函数：组合分配"""
    allocator = PortfolioAllocator()
    fused = allocator.fuse_signals(signals, method=fusion_method)
    return allocator.apply_risk_constraints(fused, current_positions)

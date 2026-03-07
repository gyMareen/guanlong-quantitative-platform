"""策略模块"""

from .base import BaseStrategy, Signal, StrategyConfig
from .mean_reversion import BollingerBandsStrategy, MeanReversionComboStrategy, RSIStrategy
from .multi_factor import MultiFactorStrategy
from .trend import BreakoutStrategy, MATrendStrategy

__all__ = [
    # Base
    "BaseStrategy",
    "Signal",
    "StrategyConfig",
    # Trend
    "MATrendStrategy",
    "BreakoutStrategy",
    # Mean Reversion
    "BollingerBandsStrategy",
    "RSIStrategy",
    "MeanReversionComboStrategy",
    # Multi-Factor
    "MultiFactorStrategy",
]


def get_strategy(name: str, **kwargs) -> BaseStrategy:
    """获取策略实例的工厂函数"""
    strategies = {
        "ma_trend": MATrendStrategy,
        "breakout": BreakoutStrategy,
        "bollinger_bands": BollingerBandsStrategy,
        "rsi": RSIStrategy,
        "mean_reversion_combo": MeanReversionComboStrategy,
        "multi_factor": MultiFactorStrategy,
    }

    if name not in strategies:
        raise ValueError(f"Unknown strategy: {name}. Available: {list(strategies.keys())}")

    return strategies[name](**kwargs)

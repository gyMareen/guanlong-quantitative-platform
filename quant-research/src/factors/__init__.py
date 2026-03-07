"""因子模块"""

from .base import Factor, PriceFactor, TechnicalFactor, VolumeFactor
from .mean_reversion import (
    MeanReversionFactor,
    OUProcessFactor,
    RSVFactor,
    WilliamsRFactor,
    ZScoreFactor,
)
from .momentum import MACD, ROCPFactor, RSI, MomentumFactor
from .volatility import ATR, BollingerBandsWidth, ParkinsonVolatility, VolatilityFactor

__all__ = [
    # Base
    "Factor",
    "PriceFactor",
    "VolumeFactor",
    "TechnicalFactor",
    # Momentum
    "MomentumFactor",
    "ROCPFactor",
    "RSI",
    "MACD",
    # Volatility
    "VolatilityFactor",
    "ATR",
    "BollingerBandsWidth",
    "ParkinsonVolatility",
    # Mean Reversion
    "ZScoreFactor",
    "MeanReversionFactor",
    "OUProcessFactor",
    "RSVFactor",
    "WilliamsRFactor",
]


def get_factor(name: str, **kwargs) -> Factor:
    """获取因子实例的工厂函数"""
    factors = {
        "momentum": MomentumFactor,
        "rocp": ROCPFactor,
        "rsi": RSI,
        "macd": MACD,
        "volatility": VolatilityFactor,
        "atr": ATR,
        "bb_width": BollingerBandsWidth,
        "parkinson_vol": ParkinsonVolatility,
        "zscore": ZScoreFactor,
        "mean_reversion": MeanReversionFactor,
        "ou_process": OUProcessFactor,
        "rsv": RSVFactor,
        "williams_r": WilliamsRFactor,
    }

    if name not in factors:
        raise ValueError(f"Unknown factor: {name}. Available: {list(factors.keys())}")

    return factors[name](**kwargs)

"""策略基类"""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

import numpy as np
import pandas as pd


class Signal(Enum):
    """交易信号"""
    BUY = 1
    SELL = -1
    HOLD = 0
    CLOSE = 2


@dataclass
class StrategyConfig:
    """策略配置"""
    name: str
    version: str = "1.0.0"
    params: Dict[str, Any] = None

    def __post_init__(self):
        if self.params is None:
            self.params = {}

    def get_param_hash(self) -> str:
        """获取参数哈希值"""
        import hashlib
        import json
        params_str = json.dumps(self.params, sort_keys=True)
        return f"sha256:{hashlib.sha256(params_str.encode()).hexdigest()[:16]}"


class BaseStrategy(ABC):
    """策略基类"""

    def __init__(self, config: Optional[StrategyConfig] = None):
        self.config = config or StrategyConfig(name=self.__class__.__name__)
        self._data: Optional[pd.DataFrame] = None

    @property
    def name(self) -> str:
        return self.config.name

    @property
    def version(self) -> str:
        return self.config.version

    def set_data(self, df: pd.DataFrame) -> None:
        """设置数据"""
        self._data = df.copy()

    @abstractmethod
    def generate_signals(self, df: pd.DataFrame) -> pd.Series:
        """生成交易信号

        Args:
            df: 包含 OHLCV 数据的 DataFrame

        Returns:
            信号序列 (1=买入, -1=卖出, 0=持有)
        """
        pass

    def compute_weights(self, signals: pd.Series) -> pd.Series:
        """根据信号计算目标权重

        默认实现：买入时全仓，卖出时空仓
        子类可以覆盖此方法实现更复杂的仓位管理
        """
        weights = signals.copy()
        weights[weights == Signal.BUY.value] = 1.0
        weights[weights == Signal.SELL.value] = 0.0
        weights[weights == Signal.HOLD.value] = np.nan
        weights = weights.ffill().fillna(0.0)
        return weights

    def compute_scores(self, df: pd.DataFrame) -> pd.Series:
        """计算评分（用于多策略融合）

        Args:
            df: 包含 OHLCV 数据的 DataFrame

        Returns:
            评分序列（正值表示看多，负值表示看空）
        """
        signals = self.generate_signals(df)
        return signals.astype(float)

    def __repr__(self) -> str:
        return f"<Strategy: {self.name} v{self.version}>"

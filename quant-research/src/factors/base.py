"""因子基类"""

from abc import ABC, abstractmethod
from typing import Optional

import numpy as np
import pandas as pd


class Factor(ABC):
    """因子基类"""

    def __init__(self, name: Optional[str] = None):
        self.name = name or self.__class__.__name__

    @abstractmethod
    def compute(self, df: pd.DataFrame) -> pd.Series:
        """计算因子值

        Args:
            df: 包含 OHLCV 数据的 DataFrame

        Returns:
            因子值序列
        """
        pass

    def __repr__(self) -> str:
        return f"<Factor: {self.name}>"


class PriceFactor(Factor):
    """价格类因子基类"""

    def compute(self, df: pd.DataFrame) -> pd.Series:
        raise NotImplementedError


class VolumeFactor(Factor):
    """成交量类因子基类"""

    def compute(self, df: pd.DataFrame) -> pd.Series:
        raise NotImplementedError


class TechnicalFactor(Factor):
    """技术指标类因子基类"""

    def compute(self, df: pd.DataFrame) -> pd.Series:
        raise NotImplementedError

"""数据接入模块"""

import logging
from abc import ABC, abstractmethod
from datetime import datetime, timedelta
from pathlib import Path
from typing import List, Optional

import numpy as np
import pandas as pd

from config import settings

logger = logging.getLogger(__name__)


class DataLoader(ABC):
    """数据加载器基类"""

    @abstractmethod
    def load(self, symbol: str, start_date: str, end_date: str) -> pd.DataFrame:
        """加载指定时间范围的数据"""
        pass

    @abstractmethod
    def load_latest(self, symbol: str, days: int = 365) -> pd.DataFrame:
        """加载最近 N 天的数据"""
        pass


class CSVDataLoader(DataLoader):
    """CSV 文件数据加载器"""

    def __init__(self, data_dir: Optional[Path] = None):
        self.data_dir = data_dir or settings.DATA_DIR / "csv"

    def load(self, symbol: str, start_date: str, end_date: str) -> pd.DataFrame:
        file_path = self.data_dir / f"{symbol}.csv"
        if not file_path.exists():
            raise FileNotFoundError(f"Data file not found: {file_path}")

        df = pd.read_csv(file_path, parse_dates=["date"])
        df = df.set_index("date")
        df = df.loc[start_date:end_date]
        return df

    def load_latest(self, symbol: str, days: int = 365) -> pd.DataFrame:
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)
        return self.load(symbol, start_date.strftime("%Y-%m-%d"), end_date.strftime("%Y-%m-%d"))


class YFinanceDataLoader(DataLoader):
    """Yahoo Finance 数据加载器"""

    def __init__(self):
        try:
            import yfinance as yf
            self.yf = yf
        except ImportError:
            raise ImportError("yfinance is required. Install with: pip install yfinance")

    def load(self, symbol: str, start_date: str, end_date: str) -> pd.DataFrame:
        ticker = self.yf.Ticker(symbol)
        df = ticker.history(start=start_date, end=end_date)

        # 标准化列名
        df = df.rename(columns={
            "Open": "open",
            "High": "high",
            "Low": "low",
            "Close": "close",
            "Volume": "volume",
        })

        df.index = pd.to_datetime(df.index)
        df.index.name = "date"
        return df

    def load_latest(self, symbol: str, days: int = 365) -> pd.DataFrame:
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)
        return self.load(symbol, start_date.strftime("%Y-%m-%d"), end_date.strftime("%Y-%m-%d"))


class AKShareDataLoader(DataLoader):
    """AKShare 数据加载器（A股数据）"""

    def __init__(self):
        try:
            import akshare as ak
            self.ak = ak
        except ImportError:
            raise ImportError("akshare is required. Install with: pip install akshare")

    def load(self, symbol: str, start_date: str, end_date: str) -> pd.DataFrame:
        # AKShare 使用格式: 000001
        df = self.ak.stock_zh_a_hist(
            symbol=symbol,
            period="daily",
            start_date=start_date.replace("-", ""),
            end_date=end_date.replace("-", ""),
            adjust="qfq"  # 前复权
        )

        df = df.rename(columns={
            "日期": "date",
            "开盘": "open",
            "最高": "high",
            "最低": "low",
            "收盘": "close",
            "成交量": "volume",
        })

        df["date"] = pd.to_datetime(df["date"])
        df = df.set_index("date")
        return df[["open", "high", "low", "close", "volume"]]

    def load_latest(self, symbol: str, days: int = 365) -> pd.DataFrame:
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)
        return self.load(symbol, start_date.strftime("%Y-%m-%d"), end_date.strftime("%Y-%m-%d"))


def get_data_loader(source: str = "yfinance") -> DataLoader:
    """获取数据加载器工厂函数"""
    loaders = {
        "csv": CSVDataLoader,
        "yfinance": YFinanceDataLoader,
        "akshare": AKShareDataLoader,
    }

    if source not in loaders:
        raise ValueError(f"Unknown data source: {source}. Available: {list(loaders.keys())}")

    return loaders[source]()

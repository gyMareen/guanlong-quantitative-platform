"""数据清洗模块"""

import logging
from typing import Optional

import numpy as np
import pandas as pd

logger = logging.getLogger(__name__)


class DataCleaner:
    """数据清洗器"""

    @staticmethod
    def remove_duplicates(df: pd.DataFrame) -> pd.DataFrame:
        """移除重复数据"""
        before = len(df)
        df = df[~df.index.duplicated(keep="last")]
        after = len(df)
        if before != after:
            logger.info(f"Removed {before - after} duplicate rows")
        return df

    @staticmethod
    def fill_missing(df: pd.DataFrame, method: str = "ffill") -> pd.DataFrame:
        """填充缺失值

        Args:
            df: 输入数据
            method: 填充方法 (ffill, bfill, interpolate, drop)
        """
        missing_count = df.isnull().sum().sum()
        if missing_count == 0:
            return df

        logger.info(f"Found {missing_count} missing values")

        if method == "ffill":
            df = df.ffill()
        elif method == "bfill":
            df = df.bfill()
        elif method == "interpolate":
            df = df.interpolate(method="linear")
        elif method == "drop":
            df = df.dropna()
        else:
            raise ValueError(f"Unknown fill method: {method}")

        return df

    @staticmethod
    def remove_outliers(
        df: pd.DataFrame,
        columns: Optional[list] = None,
        n_std: float = 3.0
    ) -> pd.DataFrame:
        """移除异常值（基于标准差）

        Args:
            df: 输入数据
            columns: 要处理的列，默认处理所有数值列
            n_std: 标准差倍数
        """
        if columns is None:
            columns = df.select_dtypes(include=[np.number]).columns.tolist()

        for col in columns:
            if col not in df.columns:
                continue

            mean = df[col].mean()
            std = df[col].std()
            lower = mean - n_std * std
            upper = mean + n_std * std

            mask = (df[col] < lower) | (df[col] > upper)
            outlier_count = mask.sum()

            if outlier_count > 0:
                logger.info(f"Removing {outlier_count} outliers from {col}")
                df = df[~mask]

        return df

    @staticmethod
    def normalize_prices(df: pd.DataFrame, adjust_column: str = "close") -> pd.DataFrame:
        """价格归一化（基于首日价格）

        Args:
            df: 输入数据
            adjust_column: 用于调整的基准列
        """
        df = df.copy()
        base_price = df[adjust_column].iloc[0]

        price_cols = ["open", "high", "low", "close"]
        for col in price_cols:
            if col in df.columns:
                df[col] = df[col] / base_price

        return df

    @staticmethod
    def calculate_returns(df: pd.DataFrame) -> pd.DataFrame:
        """计算收益率"""
        df = df.copy()
        df["returns"] = df["close"].pct_change()
        df["log_returns"] = np.log(df["close"] / df["close"].shift(1))
        return df

    def clean(
        self,
        df: pd.DataFrame,
        remove_duplicates: bool = True,
        fill_method: str = "ffill",
        calculate_returns: bool = True
    ) -> pd.DataFrame:
        """执行完整的数据清洗流程"""
        if remove_duplicates:
            df = self.remove_duplicates(df)

        df = self.fill_missing(df, method=fill_method)

        if calculate_returns:
            df = self.calculate_returns(df)

        return df


def clean_data(df: pd.DataFrame, **kwargs) -> pd.DataFrame:
    """便捷函数：清洗数据"""
    cleaner = DataCleaner()
    return cleaner.clean(df, **kwargs)

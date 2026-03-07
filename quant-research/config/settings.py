# 官龙量化一体化交易平台 - 量化研究模块配置

import os
from pathlib import Path
from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""

    # 项目路径
    BASE_DIR: Path = Path(__file__).resolve().parent.parent
    DATA_DIR: Path = BASE_DIR / "data"
    LOG_DIR: Path = BASE_DIR / "logs"

    # PostgreSQL 配置
    POSTGRES_HOST: str = Field(default="localhost", alias="POSTGRES_HOST")
    POSTGRES_PORT: int = Field(default=5432, alias="POSTGRES_PORT")
    POSTGRES_DB: str = Field(default="guanlong", alias="POSTGRES_DB")
    POSTGRES_USER: str = Field(default="postgres", alias="POSTGRES_USER")
    POSTGRES_PASSWORD: str = Field(default="postgres", alias="POSTGRES_PASSWORD")

    @property
    def DATABASE_URL(self) -> str:
        return f"postgresql://{self.POSTGRES_USER}:{self.POSTGRES_PASSWORD}@{self.POSTGRES_HOST}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}"

    # Redis 配置
    REDIS_HOST: str = Field(default="localhost", alias="REDIS_HOST")
    REDIS_PORT: int = Field(default=6379, alias="REDIS_PORT")
    REDIS_PASSWORD: Optional[str] = Field(default=None, alias="REDIS_PASSWORD")
    REDIS_DB: int = Field(default=0, alias="REDIS_DATABASE")

    # Kafka 配置
    KAFKA_BOOTSTRAP_SERVERS: str = Field(default="localhost:9092", alias="KAFKA_BOOTSTRAP_SERVERS")
    KAFKA_TOPIC_RAW: str = Field(default="futu.rebalance.raw", alias="KAFKA_TOPIC_RAW")
    KAFKA_TOPIC_QS: str = Field(default="futu.rebalance.qs", alias="KAFKA_TOPIC_QS")
    KAFKA_TOPIC_EVENTS: str = Field(default="futu.trade.events", alias="KAFKA_TOPIC_EVENTS")
    KAFKA_CONSUMER_GROUP: str = Field(default="guanlong-quant", alias="KAFKA_CONSUMER_GROUP")

    # 策略配置
    DEFAULT_STRATEGY: str = "multi_factor_v1"
    BACKTEST_INITIAL_CASH: float = 1_000_000.0
    BACKTEST_COMMISSION: float = 0.001  # 0.1%
    BACKTEST_SLIPPAGE: float = 0.0005  # 0.05%

    # 风控配置
    MAX_SINGLE_POSITION: float = 0.20  # 单票最大仓位 20%
    MAX_TOTAL_POSITION: float = 0.95   # 组合最大仓位 95%
    MIN_TRADE_AMOUNT: float = 20.0     # 最小交易金额 $20
    MAX_PRICE_DEVIATION: float = 0.04  # 最大价格偏离 4%

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"


# 全局配置实例
settings = Settings()


def ensure_dirs():
    """确保必要目录存在"""
    settings.DATA_DIR.mkdir(parents=True, exist_ok=True)
    settings.LOG_DIR.mkdir(parents=True, exist_ok=True)

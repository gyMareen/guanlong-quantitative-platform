"""量化研究模块"""

from .backtest import BacktestEngine, BacktestResult, run_backtest
from .data import DataLoader, clean_data, get_data_loader
from .factors import Factor, get_factor
from .portfolio import PortfolioAllocator, allocate_portfolio
from .signals import SignalModel, SignalProducer
from .strategies import BaseStrategy, Signal, get_strategy

__version__ = "1.0.0"

__all__ = [
    # Data
    "DataLoader",
    "get_data_loader",
    "clean_data",
    # Factors
    "Factor",
    "get_factor",
    # Strategies
    "BaseStrategy",
    "Signal",
    "get_strategy",
    # Backtest
    "BacktestEngine",
    "BacktestResult",
    "run_backtest",
    # Signals
    "SignalModel",
    "SignalProducer",
    # Portfolio
    "PortfolioAllocator",
    "allocate_portfolio",
]

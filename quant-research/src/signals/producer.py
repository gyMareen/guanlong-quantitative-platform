"""信号生产者 - Kafka 输出"""

import json
import logging
from datetime import datetime
from typing import List, Optional

from confluent_kafka import Producer

from config import settings
from ..strategies.base import BaseStrategy
from .models import SignalModel, SignalBatch

logger = logging.getLogger(__name__)


class SignalProducer:
    """信号生产者

    将策略信号发送到 Kafka
    """

    def __init__(
        self,
        bootstrap_servers: Optional[str] = None,
        topic: Optional[str] = None
    ):
        self.bootstrap_servers = bootstrap_servers or settings.KAFKA_BOOTSTRAP_SERVERS
        self.topic = topic or settings.KAFKA_TOPIC_QS

        self.producer = Producer({
            "bootstrap.servers": self.bootstrap_servers,
            "client.id": "guanlong-quant-producer",
        })

    def _delivery_callback(self, err, msg):
        """消息发送回调"""
        if err:
            logger.error(f"Message delivery failed: {err}")
        else:
            logger.debug(f"Message delivered to {msg.topic()} [{msg.partition()}]")

    def send_signal(self, signal: SignalModel) -> None:
        """发送单个信号"""
        try:
            message = signal.to_kafka_message()
            self.producer.produce(
                topic=self.topic,
                key=signal.symbol.encode("utf-8"),
                value=message.encode("utf-8"),
                callback=self._delivery_callback
            )
            self.producer.poll(0)
            logger.info(f"Sent signal: {signal.symbol} {signal.action}")
        except Exception as e:
            logger.error(f"Failed to send signal: {e}")
            raise

    def send_signals(self, signals: List[SignalModel]) -> None:
        """批量发送信号"""
        for signal in signals:
            self.send_signal(signal)
        self.producer.flush()

    def send_batch(self, batch: SignalBatch) -> None:
        """发送信号批次"""
        logger.info(f"Sending batch {batch.batch_id} with {len(batch.signals)} signals")
        self.send_signals(batch.signals)

    def close(self) -> None:
        """关闭生产者"""
        self.producer.flush()


def generate_signals_from_strategy(
    strategy: BaseStrategy,
    symbols: List[str],
    data_loader,
    start_date: str,
    end_date: str,
    min_score: float = 0.3
) -> List[SignalModel]:
    """从策略生成信号

    Args:
        strategy: 策略实例
        symbols: 股票代码列表
        data_loader: 数据加载器
        start_date: 开始日期
        end_date: 结束日期
        min_score: 最小评分阈值

    Returns:
        信号列表
    """
    signals = []

    for symbol in symbols:
        try:
            # 加载数据
            df = data_loader.load(symbol, start_date, end_date)
            if df.empty:
                continue

            # 计算评分
            scores = strategy.compute_scores(df)
            latest_score = scores.iloc[-1]

            # 生成信号
            if abs(latest_score) >= min_score:
                if latest_score > 0:
                    action = "BUY"
                    target_weight = min(latest_score, 1.0)
                else:
                    action = "SELL"
                    target_weight = max(0, 1 + latest_score)

                signal = SignalModel(
                    symbol=symbol,
                    action=action,
                    target_weight=target_weight,
                    score=float(latest_score),
                    strategy=strategy.name,
                    strategy_version=strategy.version,
                    params_hash=strategy.config.get_param_hash(),
                    source="quant_strategy",
                )
                signals.append(signal)

        except Exception as e:
            logger.error(f"Error processing {symbol}: {e}")
            continue

    return signals

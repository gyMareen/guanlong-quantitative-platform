"""信号模型"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class ActionType(str, Enum):
    """动作类型"""
    BUY = "BUY"
    SELL = "SELL"
    CLOSE = "CLOSE"
    TARGET = "TARGET"


class SignalModel(BaseModel):
    """统一信号协议"""

    symbol: str = Field(..., description="股票代码，含市场后缀")
    action: ActionType = Field(..., description="动作类型")
    target_weight: Optional[float] = Field(None, ge=0, le=1, description="目标权重")
    target_position: Optional[int] = Field(None, ge=0, description="目标持仓股数")
    score: Optional[float] = Field(None, description="量化评分")
    strategy: str = Field(..., description="策略名称")
    strategy_version: str = Field(default="1.0.0", description="策略版本")
    timestamp: datetime = Field(default_factory=datetime.utcnow, description="信号时间")
    params_hash: Optional[str] = Field(None, description="策略参数哈希")
    source: str = Field(default="quant_strategy", description="信号来源")
    note: Optional[str] = Field(None, description="备注")

    class Config:
        use_enum_values = True

    def to_kafka_message(self) -> str:
        """转换为 Kafka 消息格式"""
        import json
        return self.model_dump_json()

    @classmethod
    def from_kafka_message(cls, message: str) -> "SignalModel":
        """从 Kafka 消息解析"""
        import json
        data = json.loads(message)
        if isinstance(data["timestamp"], str):
            data["timestamp"] = datetime.fromisoformat(data["timestamp"].replace("Z", "+00:00"))
        return cls(**data)


@dataclass
class SignalBatch:
    """信号批次"""
    batch_id: str
    signals: list[SignalModel] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.utcnow)

    def add_signal(self, signal: SignalModel) -> None:
        self.signals.append(signal)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "batch_id": self.batch_id,
            "signals": [s.model_dump() for s in self.signals],
            "created_at": self.created_at.isoformat(),
        }

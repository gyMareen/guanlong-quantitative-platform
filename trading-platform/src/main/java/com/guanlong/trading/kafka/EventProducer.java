package com.guanlong.trading.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.events:futu.trade.events}")
    private String eventsTopic;

    public void sendTradeEvent(String eventType, String symbol, Map<String, Object> data) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", eventType);
            event.put("symbol", symbol);
            event.put("timestamp", System.currentTimeMillis());
            event.put("data", data);

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(eventsTopic, symbol, message);

            log.info("Sent trade event: {} - {}", eventType, symbol);
        } catch (Exception e) {
            log.error("Failed to send trade event", e);
        }
    }

    public void sendOrderCreatedEvent(String symbol, Long orderId) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        sendTradeEvent("ORDER_CREATED", symbol, data);
    }

    public void sendOrderFilledEvent(String symbol, Long orderId, Integer filledQty) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("filledQty", filledQty);
        sendTradeEvent("ORDER_FILLED", symbol, data);
    }

    public void sendRiskRejectEvent(String symbol, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        sendTradeEvent("RISK_REJECT", symbol, data);
    }

    public void sendCircuitBreakerEvent(String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        sendTradeEvent("CIRCUIT_BREAKER", "SYSTEM", data);
    }
}

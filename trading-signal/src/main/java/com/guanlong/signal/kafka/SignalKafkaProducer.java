package com.guanlong.signal.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guanlong.signal.parser.RebalanceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.raw:futu.rebalance.raw}")
    private String topic;

    public void sendRebalanceSignal(RebalanceData data) {
        try {
            String key = data.getSymbol();
            String value = buildMessage(data);

            kafkaTemplate.send(topic, key, value);
            log.info("Sent rebalance signal to Kafka: {} -> {}", topic, key);

        } catch (Exception e) {
            log.error("Failed to send rebalance signal: {}", data.getSymbol(), e);
        }
    }

    public void sendRebalanceSignals(Iterable<RebalanceData> dataList) {
        for (RebalanceData data : dataList) {
            sendRebalanceSignal(data);
        }
    }

    private String buildMessage(RebalanceData data) {
        Map<String, Object> message = new HashMap<>();
        message.put("symbol", data.getSymbol());
        message.put("action", "TARGET");
        message.put("target_weight", data.getAllocationTar());
        message.put("strategy", "futu_rebalance");
        message.put("strategy_version", "1.0.0");
        message.put("timestamp", Instant.now().toString());
        message.put("source", data.getSource());

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", data.getName());
        payload.put("allocation_cur", data.getAllocationCur());
        payload.put("allocation_tar", data.getAllocationTar());
        payload.put("ref_price", data.getRefPrice());
        payload.put("date", data.getDate());
        message.put("payload", payload);

        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }
}

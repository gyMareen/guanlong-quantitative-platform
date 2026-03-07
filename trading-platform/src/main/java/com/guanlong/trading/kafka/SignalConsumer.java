package com.guanlong.trading.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guanlong.trading.domain.Signal;
import com.guanlong.trading.service.SignalProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalConsumer {

    private final ObjectMapper objectMapper;
    private final SignalProcessor signalProcessor;

    @KafkaListener(topics = "${spring.kafka.topic.raw:futu.rebalance.raw}", groupId = "${spring.kafka.consumer.group-id:guanlong-trading}")
    public void consumeRawSignal(ConsumerRecord<String, String> record) {
        try {
            log.debug("Received raw signal: {}", record.value());
            Signal signal = objectMapper.readValue(record.value(), Signal.class);
            signal.setSource("futu_rebalance");
            signalProcessor.processSignal(signal);
        } catch (Exception e) {
            log.error("Failed to process raw signal: {}", record.value(), e);
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.qs:futu.rebalance.qs}", groupId = "${spring.kafka.consumer.group-id:guanlong-trading}")
    public void consumeQuantSignal(ConsumerRecord<String, String> record) {
        try {
            log.debug("Received quant signal: {}", record.value());
            Signal signal = objectMapper.readValue(record.value(), Signal.class);
            signal.setSource("quant_strategy");
            signalProcessor.processSignal(signal);
        } catch (Exception e) {
            log.error("Failed to process quant signal: {}", record.value(), e);
        }
    }
}

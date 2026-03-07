package com.guanlong.trading.service;

import com.guanlong.trading.domain.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalProcessor {

    // 信号缓存，用于合并同一 symbol 的多个信号
    private final Map<String, List<Signal>> signalCache = new ConcurrentHashMap<>();

    public void processSignal(Signal signal) {
        log.debug("Processing signal: {} - {} - {}", signal.getSymbol(), signal.getAction(), signal.getSource());

        // 缓存信号
        signalCache.computeIfAbsent(signal.getSymbol(), k -> new ArrayList<>()).add(signal);
    }

    public Map<String, Signal> mergeSignals() {
        // 按时间优先级合并信号
        Map<String, Signal> merged = new HashMap<>();

        for (Map.Entry<String, List<Signal>> entry : signalCache.entrySet()) {
            String symbol = entry.getKey();
            List<Signal> signals = entry.getValue();

            // 简单策略：取最新的信号
            Optional<Signal> latest = signals.stream()
                    .max(Comparator.comparing(Signal::getTimestamp));

            latest.ifPresent(signal -> merged.put(symbol, signal));
        }

        return merged;
    }

    public void clearCache() {
        signalCache.clear();
    }

    public int getCacheSize() {
        return signalCache.values().stream().mapToInt(List::size).sum();
    }
}

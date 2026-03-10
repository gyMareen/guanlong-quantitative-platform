package com.guanlong.trading.service;

import com.guanlong.trading.domain.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 信号归并器
 * 负责将多个信号融合为最终的目标权重
 */
@Slf4j
@Component
public class SignalMerger {

    /**
     * 融合信号
     * 同一股票多个信号的加权平均
     */
    public List<Signal> mergeSignals(List<Signal> signals) {
        if (signals == null || signals.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 symbol 分组
        Map<String, List<Signal>> groupedSignals = signals.stream()
                .collect(Collectors.groupingBy(Signal::getSymbol));

        List<Signal> mergedSignals = new ArrayList<>();

        for (Map.Entry<String, List<Signal>> entry : groupedSignals.entrySet()) {
            String symbol = entry.getKey();
            List<Signal> symbolSignals = entry.getValue();

            Signal mergedSignal = mergeSymbolSignals(symbol, symbolSignals);
            if (mergedSignal != null) {
                mergedSignals.add(mergedSignal);
            }
        }

        log.info("Merged {} signals into {} unique symbols", signals.size(), mergedSignals.size());
        return mergedSignals;
    }

    /**
     * 融合同一股票的多个信号
     */
    private Signal mergeSymbolSignals(String symbol, List<Signal> signals) {
        if (signals.size() == 1) {
            return signals.get(0);
        }

        // 加权平均融合
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;

        Signal.SignalBuilder builder = Signal.builder()
                .symbol(symbol);

        for (Signal signal : signals) {
            // 使用 score 作为权重
            BigDecimal weight = signal.getScore() != null
                    ? signal.getScore().abs()
                    : BigDecimal.ONE;

            BigDecimal targetWeight = signal.getTargetWeight() != null
                    ? signal.getTargetWeight()
                    : BigDecimal.ZERO;

            weightedSum = weightedSum.add(targetWeight.multiply(weight));
            totalWeight = totalWeight.add(weight);
            totalScore = totalScore.add(signal.getScore() != null ? signal.getScore() : BigDecimal.ZERO);

            // 保留最早的策略信息
            if (builder.build().getStrategy() == null) {
                builder.strategy(signal.getStrategy());
                builder.strategyVersion(signal.getStrategyVersion());
            }
        }

        // 计算加权平均权重
        BigDecimal mergedWeight = totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 归一化到 [0, 1]
        mergedWeight = mergedWeight.max(BigDecimal.ZERO).min(BigDecimal.ONE);

        return builder
                .targetWeight(mergedWeight)
                .score(totalScore.divide(BigDecimal.valueOf(signals.size()), 4, RoundingMode.HALF_UP))
                .source("merged")
                .build();
    }

    /**
     * 冲突仲裁
     * 当信号方向不一致时，选择评分最高的方向
     */
    public Signal resolveConflict(List<Signal> conflictingSignals) {
        if (conflictingSignals == null || conflictingSignals.isEmpty()) {
            return null;
        }

        // 按 score 绝对值排序
        return conflictingSignals.stream()
                .max(Comparator.comparing(s ->
                        s.getScore() != null ? s.getScore().abs() : BigDecimal.ZERO))
                .orElse(null);
    }
}

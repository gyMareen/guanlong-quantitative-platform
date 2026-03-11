package com.guanlong.signal;

import cn.hutool.core.util.IdUtil;
import com.guanlong.signal.appium.AppiumDriverManager;
import com.guanlong.signal.appium.FutuElementLocator;
import com.guanlong.signal.config.CollectorConfig;
import com.guanlong.signal.kafka.SignalKafkaProducer;
import com.guanlong.signal.parser.RebalanceData;
import com.guanlong.signal.parser.RebalanceParser;
import com.guanlong.signal.persistence.RebalanceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "collector.enabled", havingValue = "true", matchIfMissing = false)
public class RebalanceCollector {

    private final AppiumDriverManager driverManager;
    private final FutuElementLocator elementLocator;
    private final RebalanceParser parser;
    private final SignalKafkaProducer kafkaProducer;
    private final RebalanceRepository repository;
    private final CollectorConfig config;

    private volatile boolean collecting = false;

    @PostConstruct
    public void init() {
        log.info("Initializing Appium driver...");
        try {
            driverManager.initDriver();
            log.info("Appium driver initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize driver on startup", e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down collector...");
        driverManager.quitDriver();
    }

    @Scheduled(fixedDelayString = "${collector.interval:15000}")
    public void collect() {
        if (collecting) {
            log.debug("Previous collection still running, skipping...");
            return;
        }

        collecting = true;
        String batchId = generateBatchId();

        try {
            log.info("Starting collection batch: {}", batchId);

            // 确保驱动可用
            ensureDriverReady();

            // 导航到调仓历史页面
            elementLocator.navigateToRebalanceHistory();

            // 查找调仓历史条目
            List<WebElement> items = elementLocator.findRebalanceHistoryItems();

            // 解析数据
            List<RebalanceData> dataList = parser.parseItems(items);

            if (dataList.isEmpty()) {
                log.warn("No rebalance data found in this batch");
                return;
            }

            // 保存到数据库
            repository.saveAll(dataList, batchId);

            // 发送到 Kafka
            kafkaProducer.sendRebalanceSignals(dataList);

            log.info("Collection batch completed: {} items", dataList.size());

        } catch (Exception e) {
            log.error("Collection failed for batch: {}", batchId, e);
            handleCollectionError(e);
        } finally {
            collecting = false;
        }
    }

    private void ensureDriverReady() {
        if (!driverManager.isDriverAlive()) {
            log.info("Driver not alive, reinitializing...");
            driverManager.restartDriver();
        }
    }

    private void handleCollectionError(Exception e) {
        // 根据错误类型处理
        if (e.getMessage() != null && e.getMessage().contains("element")) {
            log.warn("Element not found, may need to restart App");
            // 可以考虑重启 App 或截图记录
        }
    }

    private String generateBatchId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + IdUtil.fastSimpleUUID().substring(0, 8);
    }

    public void startCollection() {
        log.info("Initializing driver for collection...");
        driverManager.initDriver();
    }

    public void stopCollection() {
        log.info("Stopping collection and quitting driver...");
        driverManager.quitDriver();
    }
}

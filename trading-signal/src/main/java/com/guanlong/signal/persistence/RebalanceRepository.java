package com.guanlong.signal.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guanlong.signal.parser.RebalanceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RebalanceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String INSERT_SQL = """
            INSERT INTO rebalance_raw
            (ts, source, name, allocation_cur, allocation_tar, symbol, ref_price, payload_json, batch_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """;

    public void save(RebalanceData data, String batchId) {
        try {
            String payloadJson = buildPayloadJson(data);

            jdbcTemplate.update(
                    INSERT_SQL,
                    Timestamp.from(Instant.now()),
                    data.getSource(),
                    data.getName(),
                    data.getAllocationCur(),
                    data.getAllocationTar(),
                    data.getSymbol(),
                    data.getRefPrice(),
                    payloadJson,
                    batchId
            );

            log.debug("Saved rebalance data: {}", data.getSymbol());

        } catch (Exception e) {
            log.error("Failed to save rebalance data: {}", data.getSymbol(), e);
        }
    }

    public void saveAll(Iterable<RebalanceData> dataList, String batchId) {
        for (RebalanceData data : dataList) {
            save(data, batchId);
        }
    }

    public boolean existsBySymbolAndBatch(String symbol, String batchId) {
        String sql = "SELECT COUNT(*) FROM rebalance_raw WHERE symbol = ? AND batch_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, symbol, batchId);
        return count != null && count > 0;
    }

    private String buildPayloadJson(RebalanceData data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", data.getName());
            payload.put("allocation_cur", data.getAllocationCur());
            payload.put("allocation_tar", data.getAllocationTar());
            payload.put("ref_price", data.getRefPrice());
            payload.put("date", data.getDate());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }
}

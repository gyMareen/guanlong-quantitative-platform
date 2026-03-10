package com.guanlong.trading.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guanlong.trading.domain.Signal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SignalRepository extends BaseMapper<Signal> {

    @Select("SELECT * FROM rebalance_qs WHERE ts >= #{startDate} AND ts < #{endDate} ORDER BY ts DESC")
    List<Signal> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Select("SELECT * FROM rebalance_qs WHERE symbol = #{symbol} ORDER BY ts DESC LIMIT #{limit}")
    List<Signal> findBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Select("SELECT * FROM rebalance_qs WHERE strategy = #{strategy} ORDER BY ts DESC")
    List<Signal> findByStrategy(@Param("strategy") String strategy);

    @Select("SELECT * FROM rebalance_qs WHERE batch_id = #{batchId} ORDER BY ts DESC")
    List<Signal> findByBatchId(@Param("batchId") String batchId);

    @Select("SELECT * FROM rebalance_qs WHERE ts >= CURRENT_DATE ORDER BY ts DESC")
    List<Signal> findTodaySignals();

    @Select("SELECT COUNT(*) FROM rebalance_qs WHERE ts >= CURRENT_DATE")
    int countTodaySignals();

    @Select("SELECT DISTINCT strategy FROM rebalance_qs WHERE strategy IS NOT NULL")
    List<String> findAllStrategies();

    @Select("SELECT DISTINCT source FROM rebalance_qs WHERE source IS NOT NULL")
    List<String> findAllSources();

    @Select("SELECT * FROM rebalance_qs WHERE symbol = #{symbol} AND ts >= #{startDate} ORDER BY ts DESC")
    List<Signal> findBySymbolAndDateRange(@Param("symbol") String symbol,
                                           @Param("startDate") LocalDateTime startDate);
}

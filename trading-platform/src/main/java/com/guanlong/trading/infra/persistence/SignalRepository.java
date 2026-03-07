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

    @Select("SELECT * FROM signals WHERE timestamp >= #{startDate} AND timestamp < #{endDate} ORDER BY timestamp DESC")
    List<Signal> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Select("SELECT * FROM signals WHERE symbol = #{symbol} ORDER BY timestamp DESC LIMIT #{limit}")
    List<Signal> findBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Select("SELECT * FROM signals WHERE strategy = #{strategy} ORDER BY timestamp DESC")
    List<Signal> findByStrategy(@Param("strategy") String strategy);

    @Select("SELECT * FROM signals WHERE batch_id = #{batchId} ORDER BY timestamp DESC")
    List<Signal> findByBatchId(@Param("batchId") String batchId);

    @Select("SELECT * FROM signals WHERE timestamp >= CURRENT_DATE ORDER BY timestamp DESC")
    List<Signal> findTodaySignals();

    @Select("SELECT COUNT(*) FROM signals WHERE timestamp >= CURRENT_DATE")
    int countTodaySignals();

    @Select("SELECT DISTINCT strategy FROM signals WHERE strategy IS NOT NULL")
    List<String> findAllStrategies();

    @Select("SELECT DISTINCT source FROM signals WHERE source IS NOT NULL")
    List<String> findAllSources();

    @Select("SELECT * FROM signals WHERE symbol = #{symbol} AND timestamp >= #{startDate} ORDER BY timestamp DESC")
    List<Signal> findBySymbolAndDateRange(@Param("symbol") String symbol,
                                           @Param("startDate") LocalDateTime startDate);
}

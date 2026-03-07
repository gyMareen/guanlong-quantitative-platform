package com.guanlong.trading.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guanlong.trading.domain.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderRepository extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE status IN ('PENDING', 'SUBMITTED', 'PARTIAL_FILLED') ORDER BY created_at DESC")
    List<Order> findActiveOrders();

    @Select("SELECT * FROM orders WHERE created_at >= #{startDate} AND created_at < #{endDate}")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Select("SELECT * FROM orders WHERE symbol = #{symbol} ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> findBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM orders WHERE status = 'REJECTED' AND created_at >= CURRENT_DATE")
    int countTodayRejectedOrders();

    @Select("SELECT COALESCE(SUM(qty * price), 0) FROM orders WHERE status = 'FILLED' AND created_at >= CURRENT_DATE")
    double sumTodayFilledAmount();
}

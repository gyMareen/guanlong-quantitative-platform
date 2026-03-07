package com.guanlong.trading.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guanlong.trading.domain.RiskRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RiskRuleRepository extends BaseMapper<RiskRule> {

    @Select("SELECT * FROM risk_rules WHERE enabled = true ORDER BY id")
    List<RiskRule> findAllEnabled();

    @Select("SELECT * FROM risk_rules WHERE rule_code = #{ruleCode}")
    RiskRule findByRuleCode(@Param("ruleCode") String ruleCode);

    @Select("SELECT * FROM risk_rules WHERE rule_code = #{ruleCode} AND enabled = true")
    RiskRule findEnabledByRuleCode(@Param("ruleCode") String ruleCode);

    @Update("UPDATE risk_rules SET enabled = #{enabled}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    @Update("UPDATE risk_rules SET params_json = #{paramsJson}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateParams(@Param("id") Long id, @Param("paramsJson") String paramsJson);

    @Select("SELECT COUNT(*) FROM risk_rules WHERE enabled = true")
    int countEnabledRules();
}

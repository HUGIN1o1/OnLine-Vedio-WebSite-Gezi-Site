package com.gezicoding.geligeli.config;

import java.util.Collections;

import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.gezicoding.geligeli.constants.SentinelResourceConstant;

import jakarta.annotation.PostConstruct;

@Configuration
public class SentinelBulletRuleConfig {

    @PostConstruct
    public void initRules() {
        initHotParamRules();
    }

    private void initHotParamRules() {
        ParamFlowRule userRateRule = new ParamFlowRule(SentinelResourceConstant.WS_BULLET_SEND);
        userRateRule.setParamIdx(0);
        userRateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userRateRule.setCount(5);
        ParamFlowRuleManager.loadRules(Collections.singletonList(userRateRule));
    }
}

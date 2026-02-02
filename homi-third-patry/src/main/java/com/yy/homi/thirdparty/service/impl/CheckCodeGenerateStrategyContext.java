package com.yy.homi.thirdparty.service.impl;

import com.yy.homi.thirdparty.interfaces.CheckCodeGenerateStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//验证码生成策略上下文
@Service
public class CheckCodeGenerateStrategyContext {
    public static final Map<String, CheckCodeGenerateStrategy> STRATEGY_MAP= new ConcurrentHashMap();

    @Autowired
    public CheckCodeGenerateStrategyContext(List<CheckCodeGenerateStrategy> checkCodeGenerateStrategyList){
        checkCodeGenerateStrategyList.stream()
                .forEach(strategy ->{
                    STRATEGY_MAP.put(strategy.getCheckCodeGenerateType(),strategy);
                });
    }

}

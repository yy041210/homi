package com.yy.homi.thirdparty.interfaces;


import com.yy.homi.common.domain.entity.R;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCheckCodeGenerateStrategy implements CheckCodeGenerateStrategy {

    protected CheckCodeGenerator checkCodeGenerator;
    protected KeyGenerator keyGenerator;  //key生成器
    protected CheckCodeStore checkCodeStore; //验证码仓库

    /**
     * 通用的核心逻辑：生成并存储
     */
    protected Map<String,String> generateAndStore(String prefix, int length, int expire) {
        String code = checkCodeGenerator.generate(length);
        String key = keyGenerator.generate(prefix);
        // 存储到 Redis (checkCodeStore 内部实现应包含 expire 逻辑)
        checkCodeStore.set(key, code, expire);
        Map<String,String> result = new HashMap<>();
        result.put("code",code);
        String[] keySplits = key.split(":");
        String uuid = keySplits[keySplits.length - 1];
        result.put("uuid",uuid);
        return result;
    }


}

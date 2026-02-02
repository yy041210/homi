package com.yy.homi.thirdparty.interfaces.impl;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;
import com.yy.homi.thirdparty.interfaces.AbstractCheckCodeGenerateStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

//发送邮箱验证码策略
@Slf4j
@Component
public class EmailCheckGenerateStrategy extends AbstractCheckCodeGenerateStrategy {
    @Override
    public String getCheckCodeGenerateType() {
        return "email";
    }

    @Override
    public R generate(CheckCodeGenerateReqDTO checkCodeGenerateReqDTO) {
        return null;
    }
}

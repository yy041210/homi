package com.yy.homi.thirdparty.interfaces.impl;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;
import com.yy.homi.thirdparty.interfaces.AbstractCheckCodeGenerateStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

//发送短信验证码策略
@Slf4j
@Component
public class SmsCheckGenerateStrategy extends AbstractCheckCodeGenerateStrategy {
    @Override
    public String getCheckCodeGenerateType() {
        return "sms";
    }

    @Override
    public R generate(CheckCodeGenerateReqDTO checkCodeGenerateReqDTO) {
        return null;
    }
}

package com.yy.homi.thirdparty.interfaces;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;

//生成验证码策略根接口
public interface CheckCodeGenerateStrategy {
    String getCheckCodeGenerateType();

    /**
     * @description 生成验证码
     */
    R generate(CheckCodeGenerateReqDTO checkCodeGenerateReqDTO);

}









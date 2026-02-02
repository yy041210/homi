package com.yy.homi.thirdparty.service;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;

public interface CheckCodeService {
    R generateCheckCode(CheckCodeGenerateReqDTO checkCodeGenerateReqDTO);

    R verifyCheckCode(String uuid, String checkCode);
}

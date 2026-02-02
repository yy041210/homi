package com.yy.homi.thirdparty.service.impl;

import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;
import com.yy.homi.thirdparty.interfaces.CheckCodeGenerateStrategy;
import com.yy.homi.thirdparty.interfaces.CheckCodeStore;
import com.yy.homi.thirdparty.service.CheckCodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CheckCodeServiceImpl implements CheckCodeService {

    @Autowired
    private CheckCodeGenerateStrategyContext checkCodeGenerateStrategyContext;
    @Resource(name = "redisCheckCodeStore")
    private CheckCodeStore checkCodeStore;

    @Override
    public R generateCheckCode(CheckCodeGenerateReqDTO checkCodeGenerateReqDTO) {
        //1.参数校验
        String checkCodeType = checkCodeGenerateReqDTO.getCheckCodeType();
        if(StrUtil.isBlank(checkCodeType)){
            return R.fail("验证码类型不能为空");
        }

        return checkCodeGenerateStrategyContext.STRATEGY_MAP.get(checkCodeType).generate(checkCodeGenerateReqDTO);
    }

    @Override
    public R verifyCheckCode(String uuid, String checkCode) {
        if (StringUtils.isAnyBlank(uuid, checkCode)) {
            return R.fail("Key或验证码不能为空");
        }
        String key = RedisConstants.THIRD.CHECK_CODE_PREFIX + uuid;
        String storedCode = checkCodeStore.get(key);
        if (storedCode == null) {
            return R.fail("验证码已失效或不存在");
        }
        if (storedCode.equalsIgnoreCase(checkCode)) {
            checkCodeStore.remove(key); // 校验成功即刻作废
            return R.ok(true); //验证吗正确
        }
        return R.fail("验证码错误"); // 验证码错误
    }
}

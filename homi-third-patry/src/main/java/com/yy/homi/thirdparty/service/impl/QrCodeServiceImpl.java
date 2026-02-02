package com.yy.homi.thirdparty.service.impl;

import cn.hutool.core.util.URLUtil;

import com.yy.homi.common.constant.ThirdPartyConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.utils.QRCodeUtil;
import com.yy.homi.thirdparty.service.QrCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${wx.open.app-id}")
    private String appId;

    @Value("${wx.open.redirect-uri}")
    private String redirectUri;

    @Override
    public R generateWeChatLoginQrCode() {
        //todo 1.生成state 雪花id ，登录时校验防止登录劫持

        //2，拼接微信官方授权 URL
        String sceneId = UUID.randomUUID().toString();
        String wxUrl = String.format(
                ThirdPartyConstants.WECHAT_QRCODE_LOGIN_URL,
                appId,
                URLUtil.encode(redirectUri),
                ThirdPartyConstants.WECHAT_LOGIN_SNS_USERINFO,
                sceneId
        );

        //3.生成二维码
        try {
            String qrCode = QRCodeUtil.createQRCode(wxUrl, 300, 300);
            if (qrCode == null) {
                return R.fail("生成二维码失败");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("qrCodeUrl",qrCode);
            result.put("sceneId",sceneId);
            return R.ok(result);
        } catch (IOException e) {
            log.error("生成二维码异常", e);
            return R.fail("生成二维码失败");
        }
    }
}

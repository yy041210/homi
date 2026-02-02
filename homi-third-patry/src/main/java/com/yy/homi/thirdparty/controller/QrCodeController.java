package com.yy.homi.thirdparty.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "二维码服务", description = "生成二维码")
@RestController
@RequestMapping("/qrcode")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    @Operation(summary = "生成微信登录二维码")
    @GetMapping("/generateWeChatLoginQrCode")
    public R generateWeChatLoginQrCode(){
        return qrCodeService.generateWeChatLoginQrCode();
    }

}

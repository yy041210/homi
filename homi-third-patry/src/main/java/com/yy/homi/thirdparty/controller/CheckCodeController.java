package com.yy.homi.thirdparty.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;
import com.yy.homi.thirdparty.service.CheckCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "验证码服务", description = "负责图片验证码、短信等验证码的生成与统一校验")
@RestController
@RequestMapping("/checkcode")
public class CheckCodeController {

    @Autowired
    private CheckCodeService checkCodeService;


    //生成验证码
    @Operation(summary = "生成验证码", description = "支持多种类型，目前主要为图片验证码(pic)")
    @PostMapping("/generateCheckCodePic")
    public R generateCheckCodePic(@RequestBody CheckCodeGenerateReqDTO checkCodeGenerateReqDTO) {
        return checkCodeService.generateCheckCode(checkCodeGenerateReqDTO);
    }

    //校验验证码
    @Operation(summary = "校验验证码", description = "统一校验接口，验证成功后通常会从缓存中删除该验证码")
    @Parameters({
            @Parameter(name = "uuid", description = "验证码唯一标识(流水号)", required = true),
            @Parameter(name = "checkCode", description = "用户输入的验证码内容", required = true)
    })
    @GetMapping("/verifyCheckCode")
    public R verifyCheckCode(@RequestParam("uuid")  String uuid,
                             @RequestParam("checkCode") String checkCode) {
        return checkCodeService.verifyCheckCode(uuid,checkCode);
    }
}

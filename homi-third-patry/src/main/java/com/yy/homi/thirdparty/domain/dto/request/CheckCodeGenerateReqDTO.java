package com.yy.homi.thirdparty.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证码生成请求参数")
public class CheckCodeGenerateReqDTO {
    /**
     * 验证码类型:pic、sms、email等
     */
    @Schema(description = "验证码类型: pic(图片), sms(短信), email(邮件)", example = "pic", required = true)
    private String checkCodeType;

    @Schema(description = "用户ID (非必填，部分业务逻辑可能需要)", example = "1001")
    private String userId;

    @Schema(description = "手机号 (发送短信验证码时必填)", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱 (发送邮件验证码时必填)", example = "homi@example.com")
    private String email;
}

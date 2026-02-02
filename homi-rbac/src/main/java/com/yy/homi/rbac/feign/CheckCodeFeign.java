package com.yy.homi.rbac.feign;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "homi-third-party",path = "/homi-third-party/checkcode")
public interface CheckCodeFeign {

    //校验验证码
    @GetMapping("/verifyCheckCode")
    R verifyCheckCode(@RequestParam("uuid") String uuid, @RequestParam("checkCode") String checkCode);

}

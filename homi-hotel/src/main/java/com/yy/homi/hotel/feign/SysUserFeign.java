package com.yy.homi.hotel.feign;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(value = "homi-rbac",path = "/homi-rbac/sysuser")
public interface SysUserFeign {
    @GetMapping("/getUserInfo")
    R getUserInfo(@RequestParam("id") String id);

}

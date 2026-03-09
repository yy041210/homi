package com.yy.homi.hotel.feign;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "homi-rbac",path = "/homi-rbac/syscity",contextId = "sysCityFeign")
public interface SysCityFeign {
    //根据ids查询对应的省名
    @PostMapping("/getNamesByIds")
    R getNamesByIds(@RequestBody List<Integer> provinceIds);
}

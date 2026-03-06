package com.yy.homi.rbac.feign;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "homi-hotel", path = "/homi-hotel/hotelbase/")
public interface HotelBaseFeign {

    @GetMapping("/getByDistrictId")
    R getByDistrictId(@RequestParam("districtId") Integer districtId);

    @GetMapping("/getByCityId")
    R getByCityId(@RequestParam("cityId") Integer cityId);

    @GetMapping("/getByProvinceId")
    R getByProvinceId(@RequestParam("provinceId") Integer provinceId);
}

package com.yy.homi.thirdparty.feigns;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 行政区划Feign客户端
 * 整合省、市、区县的查询接口
 */
@FeignClient(name = "homi-rbac", path = "/homi-rbac")
public interface RegionFeign {

    // ========== 省份接口 ==========
    @GetMapping("/sysprovince/getIdByProName")
    R getIdByProvinceName(@RequestParam("provinceName") String provinceName);

    // ========== 城市接口 ==========
    @GetMapping("/syscity/getIdByCityNameAndProId")
    R getIdByCityNameAndProvinceId(
            @RequestParam("cityName") String cityName, 
            @RequestParam("provinceId") Integer provinceId);

    // ========== 区县接口 ==========
    @GetMapping("/sysdistrict/getIdByDisNameAndCityId")
    R getIdByDistrictNameAndCityId(
            @RequestParam("districtName") String districtName, 
            @RequestParam("cityId") Integer cityId);

}
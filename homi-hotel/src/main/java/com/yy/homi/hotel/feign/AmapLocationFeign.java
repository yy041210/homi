package com.yy.homi.hotel.feign;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "homi-third-party", path = "/homi-third-party/amaplocation")
public interface AmapLocationFeign {

    @GetMapping("/regeo")
    R getAddressByLngLat(@RequestParam("lng") Double lng, @RequestParam("lat") Double lat);
}

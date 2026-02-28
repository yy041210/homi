package com.yy.homi.thirdparty.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.thirdparty.service.AmapLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/amaplocation")
public class AmapLocationController {

    @Autowired
    private AmapLocationService amapLocationService;
    /**
     * 根据经纬度获取地址信息
     * @param lng 经度
     * @param lat 纬度
     * @return 地址信息
     */
    @GetMapping("/regeo")
    public R getAddressByLngLat(
            @RequestParam("lng") Double lng,
            @RequestParam("lat") Double lat) {
        log.info("接收到经纬度查询: lng={}, lat={}", lng, lat);
        return amapLocationService.getAddressByLngLat(lng, lat);
    }


}

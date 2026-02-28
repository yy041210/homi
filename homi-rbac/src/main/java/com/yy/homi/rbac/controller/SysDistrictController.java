package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.service.SysDistrictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/sysdistrict")
public class SysDistrictController {

    @Autowired
    private SysDistrictService sysDistrictService;

    @GetMapping("/getIdByDisNameAndCityId")
    public R getIdByDisNameAndCityId(@RequestParam("districtName") @NotBlank String districtName,
                                     @RequestParam("cityId") @NotNull Integer cityId){
        return sysDistrictService.getIdByDisNameAndCityId(districtName,cityId);
    }
}

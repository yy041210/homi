package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.service.SysCityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/syscity")
public class SysCityController {

    @Autowired
    private SysCityService sysCityService;

    @GetMapping("/getIdByCityNameAndProId")
    public R getIdByCityNameAndProId(@RequestParam("cityName") @NotBlank String cityName,
                                     @RequestParam("provinceId") @NotNull Integer provinceId) {
        return sysCityService.getIdByCityNameAndProId(cityName, provinceId);
    }
}

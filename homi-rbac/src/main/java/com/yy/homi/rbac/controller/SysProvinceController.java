package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.service.SysProvinceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/sysprovince")
public class SysProvinceController {

    @Autowired
    private SysProvinceService sysProvinceService;

    @GetMapping("/getIdByProName")
    public R getIdByProName(@RequestParam("provinceName") @NotBlank String provinceName) {
        return sysProvinceService.getIdByProName(provinceName);
    }

    @GetMapping("/getAllProvinces")
    public R getAllProvinces(){
        return sysProvinceService.getAllProvinces();
    }

    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("provinceId") @NotNull Integer provinceId){
        return sysProvinceService.deleteById(provinceId);
    }
}

package com.yy.homi.rbac.controller;

import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.BusinessType;
import com.yy.homi.rbac.service.SysCityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
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

    @GetMapping("/getCitiesByProId")
    public R getCitiesByProId(@RequestParam("provinceId") @NotNull Integer provinceId){
        return sysCityService.getCitiesByProId(provinceId);
    }

    @GetMapping("/getAllCities")
    public R getAllCities(){
        return sysCityService.getAllCities();
    }

    @AutoLog(title = "地区管理-根据id删除城市",businessType = BusinessType.DELETE)
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("cityId") @NotNull Integer cityId){
        return sysCityService.deleteById(cityId);
    }


    @GetMapping("/getInfoById")
    public  R getInfoById(@RequestParam("cityId") @NotNull Integer cityId){
        return sysCityService.getInfoById(cityId);
    }

    //根据ids查询对应的省名
    @PostMapping("/getNamesByIds")
    public R getNamesByIds(@RequestBody List<Integer> cityIds){
        return sysCityService.getNamesByIds(cityIds);
    }
}

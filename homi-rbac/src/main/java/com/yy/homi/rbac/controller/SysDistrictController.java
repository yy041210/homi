package com.yy.homi.rbac.controller;

import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.BusinessType;
import com.yy.homi.rbac.service.SysDistrictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
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

    @GetMapping("/getDistrictsByCityId")
    public R getDistrictsByCityId(@RequestParam("cityId") Integer cityId){
        return sysDistrictService.getDistrictsByCityId(cityId);
    }


    @AutoLog(title = "地区管理-根据id删除区",businessType = BusinessType.DELETE)
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("districtId") @NotNull Integer districtId){
        return sysDistrictService.deleteById(districtId);
    }

    @GetMapping("/getInfoById")
    public R getInfoById(@RequestParam("districtId") @NotNull Integer districtId){
        return sysDistrictService.getInfoById(districtId);
    }


    @PostMapping("/getNamesByIds")
    public R getNamesByIds(@RequestBody List<Integer> districtIds){
        return sysDistrictService.getNamesByIds(districtIds);
    }

}

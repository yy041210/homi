package com.yy.homi.rbac.controller;

import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.BusinessType;
import com.yy.homi.rbac.service.SysProvinceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

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

    @AutoLog(title = "地区管理-根据id删除省",businessType = BusinessType.DELETE)
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("provinceId") @NotNull Integer provinceId){
        return sysProvinceService.deleteById(provinceId);
    }

    @GetMapping("/getInfoById")
    public R getInfoById(@RequestParam("provinceId") @NotNull Integer provinceId){
        return sysProvinceService.getInfoById(provinceId);
    }

    //根据ids查询对应的省名
    @PostMapping("/getNamesByIds")
    public R getNamesByIds(@RequestBody List<Integer> provinceIds){
        return sysProvinceService.getNamesByIds(provinceIds);
    }
}

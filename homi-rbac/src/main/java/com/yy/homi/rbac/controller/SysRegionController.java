package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.RegionInsertReqDTO;
import com.yy.homi.rbac.service.SysRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/sysregion")
public class SysRegionController {

    @Autowired
    private SysRegionService sysRegionService;

    @GetMapping("/getRegionTree")
    public R getRegionTree() {
        return sysRegionService.getRegionTree();
    }

    //根据名称模糊查询省市区
    @GetMapping("/searchRegionTree")
    public R searchRegionTree(@RequestParam("keyword") @NotBlank String keyword){
        return sysRegionService.searchRegionTree(keyword);
    }

    //新增省市区
    @PostMapping("/insertRegion")
    public R insertRegion(@Validated @RequestBody RegionInsertReqDTO reqDTO){
        return sysRegionService.insertRegion(reqDTO);
    }
}
package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.service.SysRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    //更具名称模糊查询省市区
    @GetMapping("/searchRegionTree")
    public R searchRegionTree(@RequestParam("keyword") @NotBlank String keyword){
        return sysRegionService.searchRegionTree(keyword);
    }
}
package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

//    /homi-hotel/analysis/hotRankList?type=hot&pageSize=10
    @GetMapping("/hotRankList")
    public R hotRankList(
            @RequestParam("type") @NotBlank(message = "排行类型不能为空！") String type,
            @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize
    ){
        return analysisService.hotRankList(type,pageSize);
    }
}

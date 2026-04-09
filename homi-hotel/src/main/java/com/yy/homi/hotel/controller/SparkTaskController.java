package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.SparkTaskPageListReqDTO;
import com.yy.homi.hotel.service.SparkTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/sparktask")
public class SparkTaskController {

    @Autowired
    private SparkTaskService sparkTaskService;

    @PostMapping("/pageList")
    public R pageList(SparkTaskPageListReqDTO reqDTO) {
        return sparkTaskService.pageList(reqDTO);
    }



}

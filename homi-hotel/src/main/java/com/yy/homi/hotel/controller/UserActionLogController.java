package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.service.UserActionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/useractionlog")
public class UserActionLogController {

    @Autowired
    private UserActionLogService userActionLogService; // 必须这样写

    @PostMapping("/insertLog")
    public R insertLog(@Validated @RequestBody UserActionLogInsertReqDTO reqDTO){
        log.info("userActionLogService is: {}", userActionLogService); // 查看是否为 null
        return userActionLogService.insertLog(reqDTO);
    }

}

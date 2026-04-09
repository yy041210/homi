package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.SysLogPageListReqDTO;
import com.yy.homi.rbac.service.SysLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/syslog")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    /**
     * 分页查询系统操作日志
     */
    @PostMapping("/pageList")
    public R pageList(@RequestBody SysLogPageListReqDTO reqDTO) {
        return sysLogService.pageList(reqDTO);
    }

    /**
     * 统计系统操作行为折线图数据
     */
    @GetMapping("/statisticLine")
    public R getStatisticLine(@RequestParam("beginTime") Long beginTime,
                              @RequestParam("endTime") Long endTime ) {
        return sysLogService.getStatisticLine(beginTime,endTime);
    }
}

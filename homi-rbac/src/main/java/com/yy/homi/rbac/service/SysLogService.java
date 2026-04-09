package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.entity.SysLog;
import com.yy.homi.rbac.domain.dto.request.SysLogPageListReqDTO;

public interface SysLogService extends IService<SysLog> {
    R pageList(SysLogPageListReqDTO reqDTO);

    R getStatisticLine(Long beginTime, Long endTime);
}

package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.SparkTaskPageListReqDTO;
import com.yy.homi.hotel.domain.entity.SparkTask;

public interface SparkTaskService extends IService<SparkTask> {
    R pageList(SparkTaskPageListReqDTO reqDTO);
}

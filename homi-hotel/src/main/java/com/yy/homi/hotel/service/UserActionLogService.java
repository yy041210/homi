package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.domain.entity.UserActionLog;

public interface UserActionLogService extends IService<UserActionLog>{


    R insertLog(UserActionLogInsertReqDTO reqDTO);

    R getViewHistory(String userId, Integer pageNum, Integer pageSize);
}
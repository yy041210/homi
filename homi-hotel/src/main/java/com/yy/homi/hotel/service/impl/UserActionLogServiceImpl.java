package com.yy.homi.hotel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.convert.UserActionLogConverter;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import com.yy.homi.hotel.mapper.UserActionLogMapper;
import com.yy.homi.hotel.service.UserActionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActionLogServiceImpl extends ServiceImpl<UserActionLogMapper, UserActionLog> implements UserActionLogService {

    @Autowired
    private UserActionLogMapper userActionLogMapper;
    @Autowired
    private UserActionLogConverter userActionLogConverter;


    @Override
    public R insertLog(UserActionLogInsertReqDTO reqDTO) {


        String hotelId = reqDTO.getHotelId();
        String actionType = reqDTO.getActionType();
        if(StrUtil.isBlank(hotelId) || StrUtil.isBlank(actionType)){
            return R.fail("酒店id或操作类型不能为空！");
        }

        UserActionLog userActionLog = userActionLogConverter.insertReqDtoToEntity(reqDTO);
        userActionLog.setActionWeight(UserActionLog.getWeightByType(userActionLog.getActionType()));
        userActionLogMapper.insert(userActionLog);

        return R.ok("插入成功！");
    }
}
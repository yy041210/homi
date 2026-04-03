package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.domain.entity.UserActionLog;

import java.util.Date;

public interface UserActionLogService extends IService<UserActionLog>{


    R insertLog(UserActionLogInsertReqDTO reqDTO);

    R getViewHistory(String userId, Integer pageNum, Integer pageSize);

    R countViewByUserId(String userId);

    R countViewHotelStarByUserId(String userId);

    R countViewCityByUserId(String userId);

    R countViewHourByUserId(String userId);

    R countViewPriceRangeByUserId(String userId);

    R getTotalTrend(String userId, Long beginTime, Long endTime);

    R countActionType(String userId);

    R countViewCommentScoreRange(String userId);

    R getPriceBoxPlotData(String userId);

    R countViewCommentCountRange(String userId);

    R getRadarStats(String userId);

    R countOpeningYearPreference(String userId);

    R getHotelRoomScaleStats(String userId);

    R getHotelFacilityCloud(String userId);

    R getRoomAreaStats(String userId);
}
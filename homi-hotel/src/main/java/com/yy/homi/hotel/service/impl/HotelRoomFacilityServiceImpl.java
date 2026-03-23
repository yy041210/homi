package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelRoomFacility;
import com.yy.homi.hotel.mapper.HotelRoomFacilityMapper;
import com.yy.homi.hotel.service.HotelRoomFacilityService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 房型设施 服务实现类
 */
@Service
public class HotelRoomFacilityServiceImpl extends ServiceImpl<HotelRoomFacilityMapper, HotelRoomFacility> implements HotelRoomFacilityService {

    @Override
    public List<HotelRoomFacility> getByRoomId(String roomId) {
        return this.list(new LambdaQueryWrapper<HotelRoomFacility>()
                .eq(HotelRoomFacility::getRoomId, roomId)
                .eq(HotelRoomFacility::getStatus, 0) // 仅查询启用状态
                .orderByAsc(HotelRoomFacility::getSeq)); // 按爬虫抓取的顺序排序
    }
}
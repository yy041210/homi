package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.hotel.domain.entity.HotelRoomFacility;
import java.util.List;

/**
 * 房型设施 服务类
 */
public interface HotelRoomFacilityService extends IService<HotelRoomFacility> {
    
    /**
     * 根据房型ID查询所有设施
     * @param roomId 房型ID
     * @return 设施列表
     */
    List<HotelRoomFacility> getByRoomId(String roomId);
}